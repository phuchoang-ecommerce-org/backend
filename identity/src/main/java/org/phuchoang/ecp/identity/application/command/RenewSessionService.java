package org.phuchoang.ecp.identity.application.command;

import org.phuchoang.ecp.identity.application.CallerContext;
import org.phuchoang.ecp.identity.application.internal.PermissionMatrix;
import org.phuchoang.ecp.identity.application.internal.RefreshTokenGenerator;
import org.phuchoang.ecp.identity.application.mapper.AccountSummary;
import org.phuchoang.ecp.identity.application.mapper.LoginResult;
import org.phuchoang.ecp.identity.application.port.AccessTokenIssuer;
import org.phuchoang.ecp.identity.application.port.AccountRepository;
import org.phuchoang.ecp.identity.application.port.AuthorizationService;
import org.phuchoang.ecp.identity.application.port.TokenRepository;
import org.phuchoang.ecp.identity.domain.Account;
import org.phuchoang.ecp.identity.domain.IdentityToken;
import org.phuchoang.ecp.identity.domain.SessionEnded;
import org.phuchoang.ecp.identity.domain.TokenType;
import org.phuchoang.ecp.sharedkernel.api.DomainException;
import org.phuchoang.ecp.sharedkernel.api.GenErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * `UC-CUS-05` — Refresh Authenticated Session (`US-CUS-05`), the story Sprint
 * 03 deliberately
 * left out (see `LoginService`'s own Javadoc). Rotation per `ADR-0016` §4:
 * presenting a refresh
 * token issues a new access/refresh pair and consumes the old one, recording
 * {@code replaced_by};
 * presenting an <em>already-consumed</em> token — reuse, whether by an attacker
 * replaying a
 * captured token or by a losing side of a concurrent double-refresh race —
 * invalidates the whole
 * chain the token belongs to, not just that one row.
 *
 * <p>
 * The caller has no valid access token at this point by definition (that's why
 * it's refreshing)
 * — so unlike every other application service, this one cannot build its
 * {@link CallerContext}
 * from the access token's claims. It re-reads the account's <em>current</em>
 * roles from
 * {@link AccountRepository} instead, which also means a role granted/revoked
 * since the last login
 * takes effect on the very next refresh, not just the next full login.
 */
@Service
public class RenewSessionService {

  private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);

  private final AccountRepository accountRepository;
  private final TokenRepository tokenRepository;
  private final AccessTokenIssuer accessTokenIssuer;
  private final AuthorizationService authorizationService;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public RenewSessionService(AccountRepository accountRepository, TokenRepository tokenRepository,
      AccessTokenIssuer accessTokenIssuer, AuthorizationService authorizationService,
      ApplicationEventPublisher events, Clock clock) {
    this.accountRepository = accountRepository;
    this.tokenRepository = tokenRepository;
    this.accessTokenIssuer = accessTokenIssuer;
    this.authorizationService = authorizationService;
    this.events = events;
    this.clock = clock;
  }

  // noRollbackFor: rejectAndInvalidateChain's whole point is a durable side
  // effect (the chain
  // invalidation) that must survive the DomainException it then throws to reject
  // *this* call —
  // Spring's default rollback-on-RuntimeException would otherwise undo the very
  // write reuse
  // detection depends on.
  @Transactional(noRollbackFor = DomainException.class)
  public LoginResult renewSession(String rawRefreshToken) {
    String tokenHash = RefreshTokenGenerator.hash(rawRefreshToken);
    IdentityToken presented = tokenRepository.findByTokenHash(tokenHash, TokenType.REFRESH)
        .orElseThrow(RenewSessionService::rejected);

    if (presented.isConsumed()) {
      // Reuse: the token has already been rotated away (or raced away — see below).
      // The
      // caller cannot tell an attacker-replay from a losing race, and doesn't need
      // to:
      // either way, the whole chain is no longer trustworthy.
      rejectAndInvalidateChain(presented);
    }
    if (presented.isExpired(clock)) {
      // Ordinary expiry, not reuse — the chain is still fine, the caller just needs
      // to log
      // in again. `openapi.yaml`'s RefreshTokenRejected response covers "invalid,
      // expired,
      // or already consumed" under one status/code — only the *side effect* differs:
      // this
      // case does not invalidate the chain.
      throw rejected();
    }

    Account account = accountRepository.findById(presented.accountId())
        .orElseThrow(RenewSessionService::rejected);
    CallerContext caller = CallerContext.of(account.id(),
        account.roles().stream().map(Enum::name).collect(java.util.stream.Collectors.toUnmodifiableSet()));
    authorizationService.assertAuthorized(caller, PermissionMatrix.RENEW_SESSION);

    // The new row must exist before the old row's replaced_by can point at it —
    // fk_identity_token_replaced_by is not deferrable (Database.md §4.1).
    UUID newTokenId = UUID.randomUUID();
    String rawNewRefreshToken = RefreshTokenGenerator.generate();
    IdentityToken newRefreshToken = IdentityToken.rotate(newTokenId, account.id(),
        RefreshTokenGenerator.hash(rawNewRefreshToken), REFRESH_TOKEN_TTL, presented.chainId(), clock);
    tokenRepository.save(newRefreshToken);

    boolean rotated = tokenRepository.rotateIfUsable(presented.id(), newTokenId, Instant.now(clock));
    if (!rotated) {
      // Lost a race against a concurrent refresh of the same token — treat
      // identically to
      // reuse: the winner already has a valid new pair, this presentation must not
      // also
      // succeed, and the chain is now suspect. The new row saved above becomes an
      // orphaned,
      // never-issued token — harmless (it was never handed to any caller) and
      // consumed
      // along with the rest of the chain by invalidateChain below.
      rejectAndInvalidateChain(presented);
    }

    AccessTokenIssuer.IssuedAccessToken accessToken = accessTokenIssuer.issue(account.id(), account.roles());

    return new LoginResult(accessToken.token(), rawNewRefreshToken, accessToken.expiresInSeconds(),
        !account.isVerified(), AccountSummary.of(account));
  }

  private void rejectAndInvalidateChain(IdentityToken presented) {
    tokenRepository.invalidateChain(presented.chainId(), Instant.now(clock));
    events.publishEvent(new SessionEnded(presented.accountId(), true, Instant.now(clock)));
    throw new DomainException(GenErrorCode.REFRESH_TOKEN_REJECTED,
        "Refresh token has already been used; the session chain has been ended.");
  }

  private static DomainException rejected() {
    // openapi.yaml's RefreshTokenRejected (ECP-GEN-4011) response covers "invalid,
    // expired,
    // or already consumed" uniformly — this endpoint never returns ECP-GEN-4010.
    return new DomainException(GenErrorCode.REFRESH_TOKEN_REJECTED, "Refresh token is invalid or expired.");
  }
}

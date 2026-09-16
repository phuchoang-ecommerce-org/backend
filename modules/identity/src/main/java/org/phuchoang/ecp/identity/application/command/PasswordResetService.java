package org.phuchoang.ecp.identity.application.command;

import org.phuchoang.ecp.identity.application.CallerContext;
import org.phuchoang.ecp.identity.application.internal.PermissionMatrix;
import org.phuchoang.ecp.identity.application.internal.RefreshTokenGenerator;
import org.phuchoang.ecp.identity.application.port.AccountRepository;
import org.phuchoang.ecp.identity.application.port.AuthorizationService;
import org.phuchoang.ecp.identity.application.port.PasswordEncoder;
import org.phuchoang.ecp.identity.application.port.TokenRepository;
import org.phuchoang.ecp.identity.domain.Account;
import org.phuchoang.ecp.identity.domain.CredentialHash;
import org.phuchoang.ecp.identity.domain.EmailAddress;
import org.phuchoang.ecp.identity.domain.IdentityToken;
import org.phuchoang.ecp.identity.domain.PasswordPolicy;
import org.phuchoang.ecp.identity.domain.PasswordResetRequested;
import org.phuchoang.ecp.identity.domain.TokenType;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;
import org.phuchoang.ecp.sharedkernel.api.error.GenErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * `UC-CUS-07` — Reset Forgotten Password (`US-CUS-07`): {@code requestPasswordReset},
 * {@code completePasswordReset}. Mirrors {@code VerifyEmailService.resendEmailVerification}'s
 * non-disclosure shape exactly (`BR-CUS-04`): the caller never learns whether the address exists.
 */
@Service
public class PasswordResetService {

    private static final Duration RESET_TOKEN_TTL = Duration.ofHours(1);

    private final AccountRepository accountRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthorizationService authorizationService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public PasswordResetService(AccountRepository accountRepository, TokenRepository tokenRepository,
            PasswordEncoder passwordEncoder, AuthorizationService authorizationService,
            ApplicationEventPublisher events, Clock clock) {
        this.accountRepository = accountRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorizationService = authorizationService;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public void requestPasswordReset(String email) {
        authorizationService.assertAuthorized(CallerContext.GUEST, PermissionMatrix.REQUEST_PASSWORD_RESET);

        Optional<Account> account = accountRepository.findByEmail(new EmailAddress(email));
        account.ifPresent(a -> {
            // A2 — at most one live token at a time.
            tokenRepository.invalidateOutstanding(a.id(), TokenType.PASSWORD_RESET);
            String rawToken = RefreshTokenGenerator.generate();
            IdentityToken token = IdentityToken.issue(UUID.randomUUID(), a.id(), TokenType.PASSWORD_RESET,
                RefreshTokenGenerator.hash(rawToken), RESET_TOKEN_TTL, clock);
            tokenRepository.save(token);
            events.publishEvent(new PasswordResetRequested(a.id(), a.email().value(), rawToken, Instant.now(clock)));
        });
        // E1 — the response is identical whether or not the address is registered; nothing is
        // dispatched and nothing is disclosed to the caller either way.
    }

    @Transactional
    public void completePasswordReset(String rawToken, String newPassword) {
        authorizationService.assertAuthorized(CallerContext.GUEST, PermissionMatrix.COMPLETE_PASSWORD_RESET);

        String tokenHash = RefreshTokenGenerator.hash(rawToken);
        IdentityToken token = tokenRepository.findByTokenHash(tokenHash, TokenType.PASSWORD_RESET)
            .filter(t -> t.isUsable(clock))
            .orElseThrow(() -> new DomainException(GenErrorCode.NOT_FOUND, "The reset link is not valid."));

        // E3 — the token is not consumed on a policy failure, so the guest can retry without a new link.
        PasswordPolicy.firstViolation(newPassword).ifPresent(violation -> {
            throw new DomainException(GenErrorCode.VALIDATION_FAILED, violation);
        });

        boolean consumed = tokenRepository.consumeIfUsable(token.id(), Instant.now(clock));
        if (!consumed) {
            throw new DomainException(GenErrorCode.NOT_FOUND, "The reset link is not valid.");
        }

        Account account = accountRepository.findById(token.accountId())
            .orElseThrow(() -> new DomainException(GenErrorCode.NOT_FOUND, "The reset link is not valid."));
        // Account closed: the token was already consumed above, so a retry cannot succeed either
        // — reported identically to an invalid link, the same non-disclosure reasoning as E1/E2.
        if (!account.isActive()) {
            throw new DomainException(GenErrorCode.NOT_FOUND, "The reset link is not valid.");
        }
        account.changePassword(new CredentialHash(passwordEncoder.encode(newPassword)), clock);
        account.verify(clock); // A1 — completing a reset proves control of the address.
        accountRepository.save(account);

        // BR-CUS-03 — every existing session ends, not just "other" ones (there is no active
        // session to preserve: the caller who completes a reset was, until now, logged out).
        tokenRepository.invalidateOutstanding(account.id(), TokenType.REFRESH);

        account.pullDomainEvents().forEach(events::publishEvent);
    }
}

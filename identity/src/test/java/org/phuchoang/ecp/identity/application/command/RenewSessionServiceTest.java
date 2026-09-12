package org.phuchoang.ecp.identity.application.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phuchoang.ecp.identity.application.internal.PermissionMatrixAuthorizationService;
import org.phuchoang.ecp.identity.application.internal.RefreshTokenGenerator;
import org.phuchoang.ecp.identity.application.mapper.LoginResult;
import org.phuchoang.ecp.identity.application.port.AccessTokenIssuer;
import org.phuchoang.ecp.identity.application.port.AccountRepository;
import org.phuchoang.ecp.identity.application.port.AuthorizationService;
import org.phuchoang.ecp.identity.application.port.TokenRepository;
import org.phuchoang.ecp.identity.domain.Account;
import org.phuchoang.ecp.identity.domain.AccountStatus;
import org.phuchoang.ecp.identity.domain.CredentialHash;
import org.phuchoang.ecp.identity.domain.EmailAddress;
import org.phuchoang.ecp.identity.domain.IdentityToken;
import org.phuchoang.ecp.identity.domain.RoleCode;
import org.phuchoang.ecp.identity.domain.TokenType;
import org.phuchoang.ecp.identity.domain.VerificationStatus;
import org.phuchoang.ecp.sharedkernel.api.DomainException;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L1 — `UC-CUS-05` (`US-CUS-05`), `ADR-0016` §4: rotation, and reuse-of-a-consumed-token
 * invalidating the whole chain.
 */
@ExtendWith(MockitoExtension.class)
class RenewSessionServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-12T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TokenRepository tokenRepository;
    @Mock
    private AccessTokenIssuer accessTokenIssuer;
    @Mock
    private ApplicationEventPublisher events;

    private RenewSessionService renewSessionService;

    @BeforeEach
    void setUp() {
        AuthorizationService authorizationService = new PermissionMatrixAuthorizationService();
        renewSessionService = new RenewSessionService(accountRepository, tokenRepository, accessTokenIssuer,
            authorizationService, events, clock);
    }

    @Test
    void aUsablePresentedTokenRotatesAndReturnsANewPair() {
        String rawToken = "raw-refresh-token";
        IdentityToken presented = usableRefreshToken(rawToken);
        Account account = activeAccount(presented.accountId());
        when(tokenRepository.findByTokenHash(RefreshTokenGenerator.hash(rawToken), TokenType.REFRESH))
            .thenReturn(Optional.of(presented));
        when(accountRepository.findById(presented.accountId())).thenReturn(Optional.of(account));
        when(tokenRepository.rotateIfUsable(eq(presented.id()), any(), eq(Instant.now(clock)))).thenReturn(true);
        when(accessTokenIssuer.issue(account.id(), account.roles()))
            .thenReturn(new AccessTokenIssuer.IssuedAccessToken("new-access-token", 900));

        LoginResult result = renewSessionService.renewSession(rawToken);

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotEqualTo(rawToken);
        verify(tokenRepository, never()).invalidateChain(any(), any());
    }

    @Test
    void presentingAnAlreadyConsumedTokenInvalidatesTheWholeChain_ADR_0016() {
        String rawToken = "raw-refresh-token";
        IdentityToken consumed = consumedRefreshToken(rawToken);
        when(tokenRepository.findByTokenHash(RefreshTokenGenerator.hash(rawToken), TokenType.REFRESH))
            .thenReturn(Optional.of(consumed));

        DomainException failure = (DomainException) catchThrowable(() -> renewSessionService.renewSession(rawToken));

        assertThat(failure.errorCode().code()).isEqualTo("ECP-GEN-4011");
        verify(tokenRepository).invalidateChain(consumed.chainId(), Instant.now(clock));
        verify(accountRepository, never()).findById(any());
    }

    @Test
    void anExpiredButNeverConsumedTokenFailsWithoutInvalidatingTheChain() {
        String rawToken = "raw-refresh-token";
        IdentityToken expired = expiredRefreshToken(rawToken);
        when(tokenRepository.findByTokenHash(RefreshTokenGenerator.hash(rawToken), TokenType.REFRESH))
            .thenReturn(Optional.of(expired));

        DomainException failure = (DomainException) catchThrowable(() -> renewSessionService.renewSession(rawToken));

        // openapi.yaml's RefreshTokenRejected response covers "invalid, expired, or already
        // consumed" uniformly under ECP-GEN-4011 — only the chain-invalidation side effect
        // differs between ordinary expiry and reuse.
        assertThat(failure.errorCode().code()).isEqualTo("ECP-GEN-4011");
        verify(tokenRepository, never()).invalidateChain(any(), any());
    }

    @Test
    void losingAConcurrentRotationRaceIsTreatedAsReuse() {
        String rawToken = "raw-refresh-token";
        IdentityToken presented = usableRefreshToken(rawToken);
        Account account = activeAccount(presented.accountId());
        when(tokenRepository.findByTokenHash(RefreshTokenGenerator.hash(rawToken), TokenType.REFRESH))
            .thenReturn(Optional.of(presented));
        when(accountRepository.findById(presented.accountId())).thenReturn(Optional.of(account));
        when(tokenRepository.rotateIfUsable(eq(presented.id()), any(), eq(Instant.now(clock)))).thenReturn(false);

        DomainException failure = (DomainException) catchThrowable(() -> renewSessionService.renewSession(rawToken));

        assertThat(failure.errorCode().code()).isEqualTo("ECP-GEN-4011");
        verify(tokenRepository).invalidateChain(presented.chainId(), Instant.now(clock));
    }

    private IdentityToken usableRefreshToken(String rawToken) {
        return IdentityToken.issue(UUID.randomUUID(), UUID.randomUUID(), TokenType.REFRESH,
            RefreshTokenGenerator.hash(rawToken), Duration.ofDays(14), clock);
    }

    private IdentityToken consumedRefreshToken(String rawToken) {
        IdentityToken issued = usableRefreshToken(rawToken);
        return new IdentityToken(issued.id(), issued.accountId(), issued.type(), issued.tokenHash(),
            issued.issuedAt(), issued.expiresAt(), Instant.now(clock), UUID.randomUUID(), issued.chainId());
    }

    private IdentityToken expiredRefreshToken(String rawToken) {
        IdentityToken issued = usableRefreshToken(rawToken);
        return new IdentityToken(issued.id(), issued.accountId(), issued.type(), issued.tokenHash(),
            issued.issuedAt().minus(Duration.ofDays(30)), issued.issuedAt().minus(Duration.ofDays(16)), null, null,
            issued.chainId());
    }

    private Account activeAccount(UUID id) {
        return Account.reconstitute(id, new EmailAddress("customer@example.com"), null, new CredentialHash("hashed"),
            "Customer", AccountStatus.ACTIVE, VerificationStatus.VERIFIED, Instant.now(clock), null, 0, 0L,
            Set.of(RoleCode.CUSTOMER), Instant.now(clock));
    }
}

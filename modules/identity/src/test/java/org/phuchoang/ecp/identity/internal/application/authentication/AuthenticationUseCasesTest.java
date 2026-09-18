package org.phuchoang.ecp.identity.internal.application.authentication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phuchoang.ecp.identity.internal.application.port.AccessTokenIssuer;
import org.phuchoang.ecp.identity.internal.application.port.DomainEventPublisher;
import org.phuchoang.ecp.identity.internal.application.port.PasswordEncoder;
import org.phuchoang.ecp.identity.internal.application.port.TokenStore;
import org.phuchoang.ecp.identity.internal.application.security.PermissionMatrixPermissionChecker;
import org.phuchoang.ecp.identity.internal.application.token.OpaqueTokens;
import org.phuchoang.ecp.identity.internal.domain.event.SessionEnded;
import org.phuchoang.ecp.identity.internal.domain.model.Account;
import org.phuchoang.ecp.identity.internal.domain.model.AccountStatus;
import org.phuchoang.ecp.identity.internal.domain.model.CredentialHash;
import org.phuchoang.ecp.identity.internal.domain.model.EmailAddress;
import org.phuchoang.ecp.identity.internal.domain.model.IdentityToken;
import org.phuchoang.ecp.identity.internal.domain.model.RoleCode;
import org.phuchoang.ecp.identity.internal.domain.model.TokenType;
import org.phuchoang.ecp.identity.internal.domain.model.VerificationStatus;
import org.phuchoang.ecp.identity.internal.domain.repository.AccountRepository;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;

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
 * L1 — `UC-CUS-03` E1/E3, `BR-CUS-04` (Gate G1 check 5): the response for an unknown account, a
 * wrong password, and a suspended account must be exactly the same {@code ECP-GEN-4010}; and
 * `UC-CUS-05` (`ADR-0016` §4): rotation, and reuse of a consumed token invalidating the chain.
 * The session manager is real over a mocked {@link TokenStore} so the persisted side effects
 * stay visible.
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationUseCasesTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-12T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TokenStore tokenStore;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AccessTokenIssuer accessTokenIssuer;
    @Mock
    private DomainEventPublisher events;

    private AuthenticationUseCases useCases;

    @BeforeEach
    void setUp() {
        useCases = new AuthenticationUseCases(accountRepository, new RefreshTokenSessionManager(tokenStore, clock),
            passwordEncoder, accessTokenIssuer, new PermissionMatrixPermissionChecker(), events, clock);
    }

    @Test
    void unknownAccountAndWrongPasswordProduceTheIdenticalFailure_BR_CUS_04() {
        when(accountRepository.findByEmail(new EmailAddress("unknown@example.com"))).thenReturn(Optional.empty());
        Account existing = activeAccount(UUID.randomUUID());
        when(accountRepository.findByEmail(new EmailAddress(existing.email().value()))).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        DomainException unknownAccountFailure = catchLoginFailure("unknown@example.com", "whatever1");
        DomainException wrongPasswordFailure = catchLoginFailure(existing.email().value(), "wrongPassword1");

        assertThat(unknownAccountFailure.errorCode().code()).isEqualTo(wrongPasswordFailure.errorCode().code());
        assertThat(unknownAccountFailure.getMessage()).isEqualTo(wrongPasswordFailure.getMessage());
        assertThat(unknownAccountFailure.errorCode().code()).isEqualTo("ECP-GEN-4010");
    }

    @Test
    void aSuspendedAccountProducesTheSameFailureAsAWrongPassword_BR_CUS_04() {
        Account suspended = Account.reconstitute(UUID.randomUUID(), new EmailAddress("suspended@example.com"), null,
            new CredentialHash("hashed"), "Suspended", AccountStatus.SUSPENDED, VerificationStatus.VERIFIED,
            Instant.now(clock), null, 0, 0L, Set.of(RoleCode.CUSTOMER), Instant.now(clock));
        when(accountRepository.findByEmail(new EmailAddress(suspended.email().value()))).thenReturn(Optional.of(suspended));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        DomainException suspendedFailure = catchLoginFailure(suspended.email().value(), "correctPassword1");

        assertThat(suspendedFailure.errorCode().code()).isEqualTo("ECP-GEN-4010");
        assertThat(suspendedFailure.getMessage()).isEqualTo("Email or password is incorrect.");
    }

    @Test
    void aSuccessfulLoginIssuesARefreshChainAndAnAccessToken() {
        Account account = activeAccount(UUID.randomUUID());
        when(accountRepository.findByEmail(new EmailAddress(account.email().value()))).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("correctPassword1", "hashed")).thenReturn(true);
        when(accessTokenIssuer.issue(account.id(), account.roles()))
            .thenReturn(new AccessTokenIssuer.IssuedAccessToken("access", 900));

        LoginResult result = useCases.logIn(new LoginCommand(account.email().value(), "correctPassword1"));

        assertThat(result.accessToken()).isEqualTo("access");
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.restricted()).isFalse();
        verify(tokenStore).save(any(IdentityToken.class));
    }

    @Test
    void aUsablePresentedTokenRotatesAndReturnsANewPair() {
        String rawToken = "raw-refresh-token";
        IdentityToken presented = usableRefreshToken(rawToken);
        Account account = activeAccount(presented.accountId());
        when(tokenStore.findByTokenHash(OpaqueTokens.hash(rawToken), TokenType.REFRESH)).thenReturn(Optional.of(presented));
        when(accountRepository.findById(presented.accountId())).thenReturn(Optional.of(account));
        when(tokenStore.rotateIfUsable(eq(presented.id()), any(), eq(Instant.now(clock)))).thenReturn(true);
        when(accessTokenIssuer.issue(account.id(), account.roles()))
            .thenReturn(new AccessTokenIssuer.IssuedAccessToken("new-access-token", 900));

        LoginResult result = useCases.renewSession(rawToken);

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isNotBlank().isNotEqualTo(rawToken);
        verify(tokenStore, never()).invalidateChain(any(), any());
    }

    @Test
    void presentingAnAlreadyConsumedTokenInvalidatesTheWholeChain_ADR_0016() {
        String rawToken = "raw-refresh-token";
        IdentityToken issued = usableRefreshToken(rawToken);
        IdentityToken consumed = new IdentityToken(issued.id(), issued.accountId(), issued.type(), issued.tokenHash(),
            issued.issuedAt(), issued.expiresAt(), Instant.now(clock), UUID.randomUUID(), issued.chainId());
        when(tokenStore.findByTokenHash(OpaqueTokens.hash(rawToken), TokenType.REFRESH)).thenReturn(Optional.of(consumed));

        DomainException failure = (DomainException) catchThrowable(() -> useCases.renewSession(rawToken));

        assertThat(failure.errorCode().code()).isEqualTo("ECP-GEN-4011");
        verify(tokenStore).invalidateChain(consumed.chainId(), Instant.now(clock));
        verify(events).publish(new SessionEnded(consumed.accountId(), true, Instant.now(clock)));
        verify(accountRepository, never()).findById(any());
    }

    @Test
    void anExpiredButNeverConsumedTokenFailsWithoutInvalidatingTheChain() {
        String rawToken = "raw-refresh-token";
        IdentityToken issued = usableRefreshToken(rawToken);
        IdentityToken expired = new IdentityToken(issued.id(), issued.accountId(), issued.type(), issued.tokenHash(),
            issued.issuedAt().minus(Duration.ofDays(30)), issued.issuedAt().minus(Duration.ofDays(16)), null, null,
            issued.chainId());
        when(tokenStore.findByTokenHash(OpaqueTokens.hash(rawToken), TokenType.REFRESH)).thenReturn(Optional.of(expired));

        DomainException failure = (DomainException) catchThrowable(() -> useCases.renewSession(rawToken));

        // openapi.yaml's RefreshTokenRejected response covers "invalid, expired, or already
        // consumed" uniformly under ECP-GEN-4011 — only the chain-invalidation side effect differs.
        assertThat(failure.errorCode().code()).isEqualTo("ECP-GEN-4011");
        verify(tokenStore, never()).invalidateChain(any(), any());
    }

    @Test
    void losingAConcurrentRotationRaceIsTreatedAsReuse() {
        String rawToken = "raw-refresh-token";
        IdentityToken presented = usableRefreshToken(rawToken);
        Account account = activeAccount(presented.accountId());
        when(tokenStore.findByTokenHash(OpaqueTokens.hash(rawToken), TokenType.REFRESH)).thenReturn(Optional.of(presented));
        when(accountRepository.findById(presented.accountId())).thenReturn(Optional.of(account));
        when(tokenStore.rotateIfUsable(eq(presented.id()), any(), eq(Instant.now(clock)))).thenReturn(false);

        DomainException failure = (DomainException) catchThrowable(() -> useCases.renewSession(rawToken));

        assertThat(failure.errorCode().code()).isEqualTo("ECP-GEN-4011");
        verify(tokenStore).invalidateChain(presented.chainId(), Instant.now(clock));
    }

    @Test
    void loggingOutRevokesTheWholeChainOfThePresentedToken_US_CUS_05() {
        String rawToken = "raw-refresh-token";
        IdentityToken presented = usableRefreshToken(rawToken);
        when(tokenStore.findByTokenHash(OpaqueTokens.hash(rawToken), TokenType.REFRESH)).thenReturn(Optional.of(presented));

        useCases.logOut(new org.phuchoang.ecp.identity.internal.application.security.CallerContext(
            presented.accountId(), Set.of(RoleCode.CUSTOMER)), rawToken);

        verify(tokenStore).invalidateChain(presented.chainId(), Instant.now(clock));
        verify(events).publish(new SessionEnded(presented.accountId(), false, Instant.now(clock)));
    }

    @Test
    void loggingOutWithAnUnknownTokenIsSuccessWithNoAction_E1() {
        when(tokenStore.findByTokenHash(any(), eq(TokenType.REFRESH))).thenReturn(Optional.empty());
        UUID accountId = UUID.randomUUID();

        useCases.logOut(new org.phuchoang.ecp.identity.internal.application.security.CallerContext(
            accountId, Set.of(RoleCode.CUSTOMER)), "stale");

        verify(tokenStore, never()).invalidateChain(any(), any());
        verify(events).publish(new SessionEnded(accountId, false, Instant.now(clock)));
    }

    private DomainException catchLoginFailure(String email, String password) {
        Throwable thrown = catchThrowable(() -> useCases.logIn(new LoginCommand(email, password)));
        assertThat(thrown).isInstanceOf(DomainException.class);
        return (DomainException) thrown;
    }

    private IdentityToken usableRefreshToken(String rawToken) {
        return IdentityToken.issue(UUID.randomUUID(), UUID.randomUUID(), TokenType.REFRESH,
            OpaqueTokens.hash(rawToken), Duration.ofDays(14), clock);
    }

    private Account activeAccount(UUID id) {
        return Account.reconstitute(id, new EmailAddress("customer@example.com"), null, new CredentialHash("hashed"),
            "Customer", AccountStatus.ACTIVE, VerificationStatus.VERIFIED, Instant.now(clock), null, 0, 0L,
            Set.of(RoleCode.CUSTOMER), Instant.now(clock));
    }
}

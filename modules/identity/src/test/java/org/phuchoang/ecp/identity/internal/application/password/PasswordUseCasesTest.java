package org.phuchoang.ecp.identity.internal.application.password;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phuchoang.ecp.identity.internal.application.authentication.RefreshTokenSessionManager;
import org.phuchoang.ecp.identity.internal.application.port.DomainEventPublisher;
import org.phuchoang.ecp.identity.internal.application.port.PasswordEncoder;
import org.phuchoang.ecp.identity.internal.application.port.TokenStore;
import org.phuchoang.ecp.identity.internal.application.security.CallerContext;
import org.phuchoang.ecp.identity.internal.application.security.PermissionChecker;
import org.phuchoang.ecp.identity.internal.application.security.PermissionMatrix;
import org.phuchoang.ecp.identity.internal.application.token.OpaqueTokens;
import org.phuchoang.ecp.identity.internal.domain.event.PasswordResetRequested;
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

/** L1 — `UC-CUS-06` (`US-CUS-06`) and `UC-CUS-07` (`US-CUS-07`): non-disclosure and single-use tokens. */
@ExtendWith(MockitoExtension.class)
class PasswordUseCasesTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-12T00:00:00Z"), ZoneOffset.UTC);
    private final UUID accountId = UUID.randomUUID();

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TokenStore tokenStore;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PermissionChecker permissions;
    @Mock
    private DomainEventPublisher events;

    private PasswordUseCases useCases() {
        return new PasswordUseCases(accountRepository, new PasswordResetTokenManager(tokenStore, clock),
            new RefreshTokenSessionManager(tokenStore, clock), passwordEncoder, permissions, events, clock);
    }

    private CallerContext caller() {
        return new CallerContext(accountId, Set.of(RoleCode.CUSTOMER));
    }

    private Account account(VerificationStatus verification) {
        return Account.reconstitute(accountId, new EmailAddress("customer@example.com"), null,
            new CredentialHash("old-hash"), "Customer", AccountStatus.ACTIVE, verification,
            verification == VerificationStatus.VERIFIED ? Instant.now(clock) : null, null, 0, 0L,
            Set.of(RoleCode.CUSTOMER), Instant.now(clock));
    }

    @Test
    void changesThePasswordAndEndsOtherSessionsByDefault() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account(VerificationStatus.VERIFIED)));
        when(passwordEncoder.matches("current", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("new-Password1", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-Password1")).thenReturn("new-hash");

        useCases().changeOwnPassword(caller(), new ChangePasswordCommand("current", "new-Password1", true));

        verify(permissions).require(any(), eq(PermissionMatrix.CHANGE_OWN_PASSWORD));
        verify(accountRepository).save(any());
        verify(tokenStore).invalidateOutstanding(accountId, TokenType.REFRESH);
        verify(events).publishFrom(any(Account.class));
    }

    @Test
    void keepsOtherSessionsWhenAsked() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account(VerificationStatus.VERIFIED)));
        when(passwordEncoder.matches("current", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("new-Password1", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-Password1")).thenReturn("new-hash");

        useCases().changeOwnPassword(caller(), new ChangePasswordCommand("current", "new-Password1", false));

        verify(tokenStore, never()).invalidateOutstanding(any(), any());
    }

    @Test
    void rejectsAnIncorrectCurrentPassword_E1() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account(VerificationStatus.VERIFIED)));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        Throwable thrown = catchThrowable(() -> useCases().changeOwnPassword(caller(),
            new ChangePasswordCommand("wrong", "new-Password1", true)));

        assertThat(thrown).isInstanceOf(DomainException.class).hasMessage("Current password is incorrect.");
        verify(accountRepository, never()).save(any());
    }

    @Test
    void rejectsANewPasswordIdenticalToTheCurrentOne_E3() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account(VerificationStatus.VERIFIED)));
        when(passwordEncoder.matches("current", "old-hash")).thenReturn(true);

        Throwable thrown = catchThrowable(() -> useCases().changeOwnPassword(caller(),
            new ChangePasswordCommand("current", "current", true)));

        assertThat(thrown).isInstanceOf(DomainException.class);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void rejectsANewPasswordThatFailsThePolicy_E2() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account(VerificationStatus.VERIFIED)));
        when(passwordEncoder.matches("current", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("weak", "old-hash")).thenReturn(false);

        Throwable thrown = catchThrowable(() -> useCases().changeOwnPassword(caller(),
            new ChangePasswordCommand("current", "weak", true)));

        assertThat(thrown).isInstanceOf(DomainException.class);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void issuesAResetTokenForARegisteredAddress() {
        when(accountRepository.findByEmail(any())).thenReturn(Optional.of(account(VerificationStatus.UNVERIFIED)));

        useCases().requestPasswordReset("customer@example.com");

        verify(tokenStore).invalidateOutstanding(accountId, TokenType.PASSWORD_RESET);
        verify(tokenStore).save(any());
        ArgumentCaptor<Object> published = ArgumentCaptor.forClass(Object.class);
        verify(events).publish(published.capture());
        assertThat(published.getValue()).isInstanceOf(PasswordResetRequested.class);
    }

    @Test
    void dispatchesNothingForAnUnregisteredAddress_E1() {
        when(accountRepository.findByEmail(any())).thenReturn(Optional.empty());

        useCases().requestPasswordReset("unknown@example.com");

        verify(tokenStore, never()).save(any());
        verify(events, never()).publish(any());
    }

    @Test
    void completingAResetChangesThePasswordVerifiesTheAccountAndEndsAllSessions() {
        IdentityToken token = resetToken("raw-token");
        when(tokenStore.findByTokenHash(OpaqueTokens.hash("raw-token"), TokenType.PASSWORD_RESET))
            .thenReturn(Optional.of(token));
        when(tokenStore.consumeIfUsable(token.id(), Instant.now(clock))).thenReturn(true);
        Account account = account(VerificationStatus.UNVERIFIED);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(passwordEncoder.encode("new-Password1")).thenReturn("new-hash");

        useCases().completePasswordReset("raw-token", "new-Password1");

        assertThat(account.isVerified()).isTrue();
        verify(accountRepository).save(account);
        verify(tokenStore).invalidateOutstanding(accountId, TokenType.REFRESH);
        verify(events).publishFrom(account);
    }

    @Test
    void doesNotConsumeTheTokenWhenTheNewPasswordFailsThePolicy_E3() {
        IdentityToken token = resetToken("raw-token");
        when(tokenStore.findByTokenHash(OpaqueTokens.hash("raw-token"), TokenType.PASSWORD_RESET))
            .thenReturn(Optional.of(token));

        Throwable thrown = catchThrowable(() -> useCases().completePasswordReset("raw-token", "weak"));

        assertThat(thrown).isInstanceOf(DomainException.class);
        verify(tokenStore, never()).consumeIfUsable(any(), any());
    }

    @Test
    void rejectsAnUnrecognisedToken_E2() {
        when(tokenStore.findByTokenHash(any(), eq(TokenType.PASSWORD_RESET))).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> useCases().completePasswordReset("bogus", "new-Password1"));

        assertThat(thrown).isInstanceOf(DomainException.class).hasMessage("The reset link is not valid.");
    }

    @Test
    void aTokenConsumedByAConcurrentCompletionIsRejectedIdentically() {
        IdentityToken token = resetToken("raw-token");
        when(tokenStore.findByTokenHash(OpaqueTokens.hash("raw-token"), TokenType.PASSWORD_RESET))
            .thenReturn(Optional.of(token));
        when(tokenStore.consumeIfUsable(token.id(), Instant.now(clock))).thenReturn(false);

        Throwable thrown = catchThrowable(() -> useCases().completePasswordReset("raw-token", "new-Password1"));

        assertThat(thrown).isInstanceOf(DomainException.class).hasMessage("The reset link is not valid.");
        verify(accountRepository, never()).save(any());
    }

    private IdentityToken resetToken(String raw) {
        return IdentityToken.issue(UUID.randomUUID(), accountId, TokenType.PASSWORD_RESET, OpaqueTokens.hash(raw),
            Duration.ofHours(1), clock);
    }
}

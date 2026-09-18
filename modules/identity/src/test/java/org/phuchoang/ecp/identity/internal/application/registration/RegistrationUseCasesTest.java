package org.phuchoang.ecp.identity.internal.application.registration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phuchoang.ecp.identity.internal.application.port.DomainEventPublisher;
import org.phuchoang.ecp.identity.internal.application.port.PasswordEncoder;
import org.phuchoang.ecp.identity.internal.application.port.TokenStore;
import org.phuchoang.ecp.identity.internal.application.security.PermissionMatrixPermissionChecker;
import org.phuchoang.ecp.identity.internal.application.token.OpaqueTokens;
import org.phuchoang.ecp.identity.internal.domain.event.AccountRegistered;
import org.phuchoang.ecp.identity.internal.domain.event.DuplicateRegistrationAttempted;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** L1 — `UC-CUS-01` (`BR-CUS-04` non-disclosure) and `UC-CUS-02` (single-use verification links). */
@ExtendWith(MockitoExtension.class)
class RegistrationUseCasesTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-12T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TokenStore tokenStore;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private DomainEventPublisher events;

    private RegistrationUseCases useCases() {
        return new RegistrationUseCases(accountRepository, new VerificationTokenManager(tokenStore, clock),
            passwordEncoder, new PermissionMatrixPermissionChecker(), events, clock);
    }

    @Test
    void registeringIssuesAVerificationTokenAndRaisesAccountRegistered() {
        when(passwordEncoder.encode("Str0ngPassword")).thenReturn("hash");
        when(accountRepository.registerNew(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCases().registerAccount(new RegisterAccountCommand("new@example.com", "Str0ngPassword", "New"));

        verify(tokenStore).save(any(IdentityToken.class));
        verify(events).publish(any(AccountRegistered.class));
    }

    @Test
    void aDuplicateEmailReturnsNormallyAndOnlyChangesTheEventRaised_BR_CUS_04() {
        when(passwordEncoder.encode("Str0ngPassword")).thenReturn("hash");
        when(accountRepository.registerNew(any()))
            .thenThrow(new AccountRepository.DuplicateEmailException(new RuntimeException("unique")));

        useCases().registerAccount(new RegisterAccountCommand("taken@example.com", "Str0ngPassword", "Dup"));

        verify(tokenStore, never()).save(any());
        verify(events).publish(new DuplicateRegistrationAttempted("taken@example.com", Instant.now(clock)));
    }

    @Test
    void aWeakPasswordIsRejectedBeforeAnyAccountIsCreated_E2() {
        Throwable thrown = catchThrowable(() -> useCases().registerAccount(
            new RegisterAccountCommand("new@example.com", "weak", "New")));

        assertThat(thrown).isInstanceOf(DomainException.class);
        verify(accountRepository, never()).registerNew(any());
    }

    @Test
    void verifyingConsumesTheTokenAndVerifiesTheAccount() {
        UUID accountId = UUID.randomUUID();
        IdentityToken token = verificationToken("raw", accountId);
        Account account = unverified(accountId);
        when(tokenStore.findByTokenHash(OpaqueTokens.hash("raw"), TokenType.EMAIL_VERIFICATION)).thenReturn(Optional.of(token));
        when(tokenStore.consumeIfUsable(token.id(), Instant.now(clock))).thenReturn(true);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        useCases().verifyEmailAddress("raw");

        assertThat(account.isVerified()).isTrue();
        verify(accountRepository).save(account);
        verify(events).publishFrom(account);
    }

    @Test
    void anExpiredLinkAndAConsumedLinkAreRejectedIdentically_E1_E2() {
        UUID accountId = UUID.randomUUID();
        IdentityToken issued = verificationToken("raw", accountId);
        IdentityToken expired = new IdentityToken(issued.id(), accountId, issued.type(), issued.tokenHash(),
            issued.issuedAt().minus(Duration.ofDays(2)), issued.issuedAt().minus(Duration.ofDays(1)), null, null, null);
        when(tokenStore.findByTokenHash(OpaqueTokens.hash("raw"), TokenType.EMAIL_VERIFICATION))
            .thenReturn(Optional.of(expired));
        IdentityToken usable = verificationToken("raced", accountId);
        when(tokenStore.findByTokenHash(OpaqueTokens.hash("raced"), TokenType.EMAIL_VERIFICATION))
            .thenReturn(Optional.of(usable));
        when(tokenStore.consumeIfUsable(usable.id(), Instant.now(clock))).thenReturn(false);

        DomainException expiredFailure = (DomainException) catchThrowable(() -> useCases().verifyEmailAddress("raw"));
        DomainException racedFailure = (DomainException) catchThrowable(() -> useCases().verifyEmailAddress("raced"));

        assertThat(expiredFailure.errorCode().code()).isEqualTo("ECP-GEN-4040").isEqualTo(racedFailure.errorCode().code());
        assertThat(expiredFailure.getMessage()).isEqualTo(racedFailure.getMessage());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void resendingForAnUnknownOrAlreadyVerifiedAddressDispatchesNothing() {
        when(accountRepository.findByEmail(new EmailAddress("unknown@example.com"))).thenReturn(Optional.empty());

        useCases().resendEmailVerification("unknown@example.com");

        verify(tokenStore, never()).save(any());
        verify(events, never()).publish(any());
    }

    private IdentityToken verificationToken(String raw, UUID accountId) {
        return IdentityToken.issue(UUID.randomUUID(), accountId, TokenType.EMAIL_VERIFICATION, OpaqueTokens.hash(raw),
            Duration.ofHours(24), clock);
    }

    private Account unverified(UUID accountId) {
        return Account.reconstitute(accountId, new EmailAddress("customer@example.com"), null,
            new CredentialHash("hash"), "Customer", AccountStatus.ACTIVE, VerificationStatus.UNVERIFIED, null, null,
            0, 0L, Set.of(RoleCode.CUSTOMER), Instant.now(clock));
    }
}

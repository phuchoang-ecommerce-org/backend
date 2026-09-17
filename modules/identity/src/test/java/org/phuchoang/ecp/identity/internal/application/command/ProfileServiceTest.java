package org.phuchoang.ecp.identity.internal.application.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.phuchoang.ecp.identity.internal.application.command.model.UpdateProfileCommand;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phuchoang.ecp.identity.internal.application.CallerContext;
import org.phuchoang.ecp.identity.internal.application.mapper.AccountSummary;
import org.phuchoang.ecp.identity.internal.domain.repository.AccountRepository;
import org.phuchoang.ecp.identity.internal.application.port.AuthorizationService;
import org.phuchoang.ecp.identity.internal.application.port.TokenStore;
import org.phuchoang.ecp.identity.internal.domain.model.Account;
import org.phuchoang.ecp.identity.internal.domain.model.AccountStatus;
import org.phuchoang.ecp.identity.internal.domain.model.CredentialHash;
import org.phuchoang.ecp.identity.internal.domain.model.EmailAddress;
import org.phuchoang.ecp.identity.internal.domain.model.RoleCode;
import org.phuchoang.ecp.identity.internal.domain.model.VerificationStatus;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** L1 — `UC-CUS-08` (`US-CUS-08`). */
@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-12T00:00:00Z"), ZoneOffset.UTC);
    private final UUID accountId = UUID.randomUUID();

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TokenStore tokenRepository;
    @Mock
    private AuthorizationService authorizationService;
    @Mock
    private ApplicationEventPublisher events;

    private ProfileService service() {
        return new ProfileService(accountRepository, tokenRepository, authorizationService, events, clock);
    }

    private Account account() {
        return Account.reconstitute(accountId, new EmailAddress("customer@example.com"), null,
            new CredentialHash("hash"), "Customer", AccountStatus.ACTIVE, VerificationStatus.VERIFIED,
            Instant.now(clock), null, 0, 0L, Set.of(RoleCode.CUSTOMER), Instant.now(clock));
    }

    @Test
    void getOwnAccountReturnsTheCallersOwnAccount() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account()));

        AccountSummary summary = service().getOwnAccount(new CallerContext(accountId, Set.of(RoleCode.CUSTOMER)));

        assertThat(summary.email()).isEqualTo("customer@example.com");
    }

    @Test
    void updatingOnlyDisplayNameDoesNotStartAnEmailVerificationCycle_A2() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account()));

        service().updateOwnProfile(new CallerContext(accountId, Set.of(RoleCode.CUSTOMER)),
            new UpdateProfileCommand("New Name", null));

        org.mockito.Mockito.verify(tokenRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void changingEmailHoldsItPendingAndIssuesAVerificationToken_A1() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account()));
        when(accountRepository.findByEmail(new EmailAddress("new@example.com"))).thenReturn(Optional.empty());

        AccountSummary summary = service().updateOwnProfile(new CallerContext(accountId, Set.of(RoleCode.CUSTOMER)),
            new UpdateProfileCommand(null, "new@example.com"));

        assertThat(summary.email()).isEqualTo("customer@example.com");
        assertThat(summary.pendingEmail()).isEqualTo("new@example.com");
        org.mockito.Mockito.verify(tokenRepository).save(any());
    }

    @Test
    void rejectsAnEmailAlreadyRegisteredToAnotherAccountWithoutDisclosingWhy_E1() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account()));
        Account other = Account.reconstitute(UUID.randomUUID(), new EmailAddress("taken@example.com"), null,
            new CredentialHash("hash"), "Other", AccountStatus.ACTIVE, VerificationStatus.VERIFIED, null, null, 0,
            0L, Set.of(RoleCode.CUSTOMER), Instant.now(clock));
        when(accountRepository.findByEmail(new EmailAddress("taken@example.com"))).thenReturn(Optional.of(other));

        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(() -> service().updateOwnProfile(
            new CallerContext(accountId, Set.of(RoleCode.CUSTOMER)),
            new UpdateProfileCommand(null, "taken@example.com")));

        assertThat(thrown).isInstanceOf(DomainException.class);
    }
}

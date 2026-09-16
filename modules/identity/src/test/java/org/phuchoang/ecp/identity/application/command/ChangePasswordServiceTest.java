package org.phuchoang.ecp.identity.application.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phuchoang.ecp.identity.application.CallerContext;
import org.phuchoang.ecp.identity.application.internal.PermissionMatrix;
import org.phuchoang.ecp.identity.application.port.AccountRepository;
import org.phuchoang.ecp.identity.application.port.AuthorizationService;
import org.phuchoang.ecp.identity.application.port.PasswordEncoder;
import org.phuchoang.ecp.identity.application.port.TokenRepository;
import org.phuchoang.ecp.identity.domain.Account;
import org.phuchoang.ecp.identity.domain.AccountStatus;
import org.phuchoang.ecp.identity.domain.CredentialHash;
import org.phuchoang.ecp.identity.domain.EmailAddress;
import org.phuchoang.ecp.identity.domain.RoleCode;
import org.phuchoang.ecp.identity.domain.TokenType;
import org.phuchoang.ecp.identity.domain.VerificationStatus;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
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

/** L1 — `UC-CUS-06` (`US-CUS-06`). */
@ExtendWith(MockitoExtension.class)
class ChangePasswordServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-12T00:00:00Z"), ZoneOffset.UTC);
    private final UUID accountId = UUID.randomUUID();

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TokenRepository tokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthorizationService authorizationService;
    @Mock
    private ApplicationEventPublisher events;

    private ChangePasswordService service;

    private ChangePasswordService service() {
        return new ChangePasswordService(accountRepository, tokenRepository, passwordEncoder, authorizationService,
            events, clock);
    }

    private Account account() {
        return Account.reconstitute(accountId, new EmailAddress("customer@example.com"), null,
            new CredentialHash("old-hash"), "Customer", AccountStatus.ACTIVE, VerificationStatus.VERIFIED,
            Instant.now(clock), null, 0, 0L, Set.of(RoleCode.CUSTOMER), Instant.now(clock));
    }

    @Test
    void changesThePasswordAndEndsOtherSessionsByDefault() {
        service = service();
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account()));
        when(passwordEncoder.matches("current", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("new-Password1", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-Password1")).thenReturn("new-hash");

        service.changeOwnPassword(new CallerContext(accountId, Set.of(RoleCode.CUSTOMER)),
            new ChangePasswordCommand("current", "new-Password1", true));

        verify(authorizationService).assertAuthorized(any(), org.mockito.ArgumentMatchers.eq(
            PermissionMatrix.CHANGE_OWN_PASSWORD));
        verify(accountRepository).save(any());
        verify(tokenRepository).invalidateOutstanding(accountId, TokenType.REFRESH);
    }

    @Test
    void keepsOtherSessionsWhenAsked() {
        service = service();
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account()));
        when(passwordEncoder.matches("current", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("new-Password1", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-Password1")).thenReturn("new-hash");

        service.changeOwnPassword(new CallerContext(accountId, Set.of(RoleCode.CUSTOMER)),
            new ChangePasswordCommand("current", "new-Password1", false));

        verify(tokenRepository, never()).invalidateOutstanding(any(), any());
    }

    @Test
    void rejectsAnIncorrectCurrentPassword_E1() {
        service = service();
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account()));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        Throwable thrown = catchThrowable(() -> service.changeOwnPassword(
            new CallerContext(accountId, Set.of(RoleCode.CUSTOMER)),
            new ChangePasswordCommand("wrong", "new-Password1", true)));

        assertThat(thrown).isInstanceOf(DomainException.class);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void rejectsANewPasswordIdenticalToTheCurrentOne_E3() {
        service = service();
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account()));
        when(passwordEncoder.matches("current", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("current", "old-hash")).thenReturn(true);

        Throwable thrown = catchThrowable(() -> service.changeOwnPassword(
            new CallerContext(accountId, Set.of(RoleCode.CUSTOMER)),
            new ChangePasswordCommand("current", "current", true)));

        assertThat(thrown).isInstanceOf(DomainException.class);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void rejectsANewPasswordThatFailsThePolicy_E2() {
        service = service();
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account()));
        when(passwordEncoder.matches("current", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("weak", "old-hash")).thenReturn(false);

        Throwable thrown = catchThrowable(() -> service.changeOwnPassword(
            new CallerContext(accountId, Set.of(RoleCode.CUSTOMER)),
            new ChangePasswordCommand("current", "weak", true)));

        assertThat(thrown).isInstanceOf(DomainException.class);
        verify(accountRepository, never()).save(any());
    }
}

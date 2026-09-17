package org.phuchoang.ecp.identity.internal.application.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.phuchoang.ecp.identity.internal.application.command.model.LoginCommand;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phuchoang.ecp.identity.internal.application.internal.PermissionMatrixAuthorizationService;
import org.phuchoang.ecp.identity.internal.application.port.AccessTokenIssuer;
import org.phuchoang.ecp.identity.internal.domain.repository.AccountRepository;
import org.phuchoang.ecp.identity.internal.application.port.AuthorizationService;
import org.phuchoang.ecp.identity.internal.application.port.PasswordEncoder;
import org.phuchoang.ecp.identity.internal.application.port.TokenStore;
import org.phuchoang.ecp.identity.internal.domain.model.Account;
import org.phuchoang.ecp.identity.internal.domain.model.AccountStatus;
import org.phuchoang.ecp.identity.internal.domain.model.CredentialHash;
import org.phuchoang.ecp.identity.internal.domain.model.EmailAddress;
import org.phuchoang.ecp.identity.internal.domain.model.RoleCode;
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

/**
 * L1 — `UC-CUS-03` E1/E3, `BR-CUS-04`: the response for an unknown account, a wrong password, and
 * a suspended account must be exactly the same {@code ECP-GEN-4010} with the exact same message —
 * this is Gate G1 check 5.
 */
@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-12T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TokenStore tokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AccessTokenIssuer accessTokenIssuer;
    @Mock
    private ApplicationEventPublisher events;

    private LoginService loginService;

    @BeforeEach
    void setUp() {
        AuthorizationService authorizationService = new PermissionMatrixAuthorizationService();
        loginService = new LoginService(accountRepository, tokenRepository, passwordEncoder, accessTokenIssuer,
            authorizationService, events, clock);
    }

    @Test
    void unknownAccountAndWrongPasswordProduceTheIdenticalFailure_BR_CUS_04() {
        when(accountRepository.findByEmail(new EmailAddress("unknown@example.com"))).thenReturn(Optional.empty());
        Account existing = activeAccount();
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
        Account suspended = suspendedAccount();
        when(accountRepository.findByEmail(new EmailAddress(suspended.email().value()))).thenReturn(Optional.of(suspended));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        DomainException suspendedFailure = catchLoginFailure(suspended.email().value(), "correctPassword1");

        assertThat(suspendedFailure.errorCode().code()).isEqualTo("ECP-GEN-4010");
        assertThat(suspendedFailure.getMessage()).isEqualTo("Email or password is incorrect.");
    }

    private DomainException catchLoginFailure(String email, String password) {
        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(
            () -> loginService.logIn(new LoginCommand(email, password)));
        assertThat(thrown).isInstanceOf(DomainException.class);
        return (DomainException) thrown;
    }

    private Account activeAccount() {
        return Account.reconstitute(UUID.randomUUID(), new EmailAddress("customer@example.com"), null,
            new CredentialHash("hashed"), "Customer", AccountStatus.ACTIVE,
            org.phuchoang.ecp.identity.internal.domain.model.VerificationStatus.VERIFIED, Instant.now(clock), null, 0, 0L,
            Set.of(RoleCode.CUSTOMER), Instant.now(clock));
    }

    private Account suspendedAccount() {
        return Account.reconstitute(UUID.randomUUID(), new EmailAddress("suspended@example.com"), null,
            new CredentialHash("hashed"), "Suspended", AccountStatus.SUSPENDED,
            org.phuchoang.ecp.identity.internal.domain.model.VerificationStatus.VERIFIED, Instant.now(clock), null, 0, 0L,
            Set.of(RoleCode.CUSTOMER), Instant.now(clock));
    }
}

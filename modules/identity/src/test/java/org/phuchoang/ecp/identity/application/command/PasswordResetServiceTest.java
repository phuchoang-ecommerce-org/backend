package org.phuchoang.ecp.identity.application.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phuchoang.ecp.identity.application.internal.RefreshTokenGenerator;
import org.phuchoang.ecp.identity.application.port.AccountRepository;
import org.phuchoang.ecp.identity.application.port.AuthorizationService;
import org.phuchoang.ecp.identity.application.port.PasswordEncoder;
import org.phuchoang.ecp.identity.application.port.TokenRepository;
import org.phuchoang.ecp.identity.domain.Account;
import org.phuchoang.ecp.identity.domain.AccountStatus;
import org.phuchoang.ecp.identity.domain.CredentialHash;
import org.phuchoang.ecp.identity.domain.EmailAddress;
import org.phuchoang.ecp.identity.domain.IdentityToken;
import org.phuchoang.ecp.identity.domain.PasswordResetRequested;
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

/** L1 — `UC-CUS-07` (`US-CUS-07`): non-disclosure (`BR-CUS-04`) and single-use tokens. */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

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

    private PasswordResetService service() {
        return new PasswordResetService(accountRepository, tokenRepository, passwordEncoder, authorizationService,
            events, clock);
    }

    private Account account() {
        return Account.reconstitute(accountId, new EmailAddress("customer@example.com"), null,
            new CredentialHash("old-hash"), "Customer", AccountStatus.ACTIVE, VerificationStatus.UNVERIFIED, null,
            null, 0, 0L, Set.of(RoleCode.CUSTOMER), Instant.now(clock));
    }

    @Test
    void issuesAResetTokenForARegisteredAddress() {
        when(accountRepository.findByEmail(any())).thenReturn(Optional.of(account()));

        service().requestPasswordReset("customer@example.com");

        verify(tokenRepository).invalidateOutstanding(accountId, TokenType.PASSWORD_RESET);
        verify(tokenRepository).save(any());
        ArgumentCaptor<Object> published = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(published.capture());
        assertThat(published.getValue()).isInstanceOf(PasswordResetRequested.class);
    }

    @Test
    void dispatchesNothingForAnUnregisteredAddress_E1() {
        when(accountRepository.findByEmail(any())).thenReturn(Optional.empty());

        service().requestPasswordReset("unknown@example.com");

        verify(tokenRepository, never()).save(any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void completingAResetChangesThePasswordVerifiesTheAccountAndEndsAllSessions() {
        IdentityToken token = IdentityToken.issue(UUID.randomUUID(), accountId, TokenType.PASSWORD_RESET,
            RefreshTokenGenerator.hash("raw-token"), Duration.ofHours(1), clock);
        when(tokenRepository.findByTokenHash(RefreshTokenGenerator.hash("raw-token"), TokenType.PASSWORD_RESET))
            .thenReturn(Optional.of(token));
        when(tokenRepository.consumeIfUsable(token.id(), Instant.now(clock))).thenReturn(true);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account()));
        when(passwordEncoder.encode("new-Password1")).thenReturn("new-hash");

        service().completePasswordReset("raw-token", "new-Password1");

        verify(accountRepository).save(any());
        verify(tokenRepository).invalidateOutstanding(accountId, TokenType.REFRESH);
    }

    @Test
    void doesNotConsumeTheTokenWhenTheNewPasswordFailsThePolicy_E3() {
        IdentityToken token = IdentityToken.issue(UUID.randomUUID(), accountId, TokenType.PASSWORD_RESET,
            RefreshTokenGenerator.hash("raw-token"), Duration.ofHours(1), clock);
        when(tokenRepository.findByTokenHash(RefreshTokenGenerator.hash("raw-token"), TokenType.PASSWORD_RESET))
            .thenReturn(Optional.of(token));

        Throwable thrown = catchThrowable(() -> service().completePasswordReset("raw-token", "weak"));

        assertThat(thrown).isInstanceOf(DomainException.class);
        verify(tokenRepository, never()).consumeIfUsable(any(), any());
    }

    @Test
    void rejectsAnUnrecognisedToken_E2() {
        when(tokenRepository.findByTokenHash(any(), eq(TokenType.PASSWORD_RESET))).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> service().completePasswordReset("bogus", "new-Password1"));

        assertThat(thrown).isInstanceOf(DomainException.class);
    }
}

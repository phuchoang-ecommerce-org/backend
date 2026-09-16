package org.phuchoang.ecp.identity.application.command;

import org.phuchoang.ecp.identity.application.CallerContext;
import org.phuchoang.ecp.identity.application.internal.PermissionMatrix;
import org.phuchoang.ecp.identity.application.internal.RefreshTokenGenerator;
import org.phuchoang.ecp.identity.application.internal.VerificationTokenIssuing;
import org.phuchoang.ecp.identity.application.port.AccountRepository;
import org.phuchoang.ecp.identity.application.port.AuthorizationService;
import org.phuchoang.ecp.identity.application.port.TokenRepository;
import org.phuchoang.ecp.identity.domain.Account;
import org.phuchoang.ecp.identity.domain.EmailAddress;
import org.phuchoang.ecp.identity.domain.EmailVerificationResent;
import org.phuchoang.ecp.identity.domain.IdentityToken;
import org.phuchoang.ecp.identity.domain.TokenType;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;
import org.phuchoang.ecp.sharedkernel.api.error.GenErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

/**
 * `UC-CUS-02` — Verify Email Address (`US-CUS-02`): {@code verifyEmailAddress} and
 * {@code resendEmailVerification}.
 */
@Service
public class VerifyEmailService {

    private final AccountRepository accountRepository;
    private final TokenRepository tokenRepository;
    private final AuthorizationService authorizationService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public VerifyEmailService(AccountRepository accountRepository, TokenRepository tokenRepository,
            AuthorizationService authorizationService, ApplicationEventPublisher events, Clock clock) {
        this.accountRepository = accountRepository;
        this.tokenRepository = tokenRepository;
        this.authorizationService = authorizationService;
        this.events = events;
        this.clock = clock;
    }

    /**
     * E1 (expired) and E2 (unrecognised/already consumed) are reported identically as
     * {@code ECP-GEN-4040} — the caller cannot tell an expired link from a fabricated one, the
     * same non-disclosure reasoning `BR-CUS-04` already applies to duplicate registration.
     */
    @Transactional
    public void verifyEmailAddress(String rawToken) {
        authorizationService.assertAuthorized(CallerContext.GUEST, PermissionMatrix.VERIFY_EMAIL_ADDRESS);

        String tokenHash = RefreshTokenGenerator.hash(rawToken);
        IdentityToken token = tokenRepository.findByTokenHash(tokenHash, TokenType.EMAIL_VERIFICATION)
            .filter(t -> t.isUsable(clock))
            .orElseThrow(() -> new DomainException(GenErrorCode.NOT_FOUND, "The verification link is not valid."));

        boolean consumed = tokenRepository.consumeIfUsable(token.id(), java.time.Instant.now(clock));
        if (!consumed) {
            // Lost the race against a concurrent use of the same token — same outcome as E2.
            throw new DomainException(GenErrorCode.NOT_FOUND, "The verification link is not valid.");
        }

        Account account = accountRepository.findById(token.accountId())
            .orElseThrow(() -> new DomainException(GenErrorCode.NOT_FOUND, "The verification link is not valid."));
        if (account.pendingEmail() != null) {
            // UC-CUS-08 A1 completion: this token was issued for an email-change request, not the
            // initial registration — promote the pending address instead of re-verifying the account.
            account.confirmPendingEmail(clock);
        } else {
            account.verify(clock); // A1 — idempotent if already verified.
        }
        accountRepository.save(account);
        account.pullDomainEvents().forEach(events::publishEvent);
    }

    /**
     * `UC-CUS-02` A2 — non-disclosive and rate-limited the same way as registration
     * (`BR-CUS-04`, `NFR-SEC-05`): the response never confirms whether the address exists.
     */
    @Transactional
    public void resendEmailVerification(String email) {
        authorizationService.assertAuthorized(CallerContext.GUEST, PermissionMatrix.RESEND_EMAIL_VERIFICATION);

        Optional<Account> account = accountRepository.findByEmail(new EmailAddress(email));
        account.filter(a -> !a.isVerified()).ifPresent(a -> {
            String rawToken = VerificationTokenIssuing.issue(tokenRepository, a.id(), clock);
            events.publishEvent(new EmailVerificationResent(a.id(), a.email().value(), rawToken, java.time.Instant.now(clock)));
        });
    }
}

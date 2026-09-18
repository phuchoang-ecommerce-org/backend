package org.phuchoang.ecp.identity.internal.application.registration;

import org.phuchoang.ecp.identity.internal.application.IdentityErrors;
import org.phuchoang.ecp.identity.internal.application.port.DomainEventPublisher;
import org.phuchoang.ecp.identity.internal.application.port.PasswordEncoder;
import org.phuchoang.ecp.identity.internal.application.security.CallerContext;
import org.phuchoang.ecp.identity.internal.application.security.PermissionChecker;
import org.phuchoang.ecp.identity.internal.application.security.PermissionMatrix;
import org.phuchoang.ecp.identity.internal.domain.event.AccountRegistered;
import org.phuchoang.ecp.identity.internal.domain.event.DuplicateRegistrationAttempted;
import org.phuchoang.ecp.identity.internal.domain.event.EmailVerificationResent;
import org.phuchoang.ecp.identity.internal.domain.model.Account;
import org.phuchoang.ecp.identity.internal.domain.model.CredentialHash;
import org.phuchoang.ecp.identity.internal.domain.model.EmailAddress;
import org.phuchoang.ecp.identity.internal.domain.model.PasswordPolicy;
import org.phuchoang.ecp.identity.internal.domain.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Registration and email-address verification: `UC-CUS-01` (`US-CUS-01`) and `UC-CUS-02`
 * (`US-CUS-02`). Duplicate email (E1) and success both return normally: {@code BR-CUS-04}
 * requires the response to never disclose which case occurred, so the distinction is made only
 * in which event is raised, never in the outcome the controller sees.
 */
@Service
public class RegistrationUseCases {

    private final AccountRepository accounts;
    private final VerificationTokenManager verificationTokens;
    private final PasswordEncoder passwordEncoder;
    private final PermissionChecker permissions;
    private final DomainEventPublisher events;
    private final Clock clock;

    public RegistrationUseCases(AccountRepository accounts, VerificationTokenManager verificationTokens,
            PasswordEncoder passwordEncoder, PermissionChecker permissions, DomainEventPublisher events,
            Clock clock) {
        this.accounts = accounts;
        this.verificationTokens = verificationTokens;
        this.passwordEncoder = passwordEncoder;
        this.permissions = permissions;
        this.events = events;
        this.clock = clock;
    }

    /**
     * {@code registerNew} below deliberately runs in its own REQUIRES_NEW transaction, since a
     * duplicate-email failure must roll back only the attempted insert, never poison the
     * surrounding one (see {@link AccountRepository#registerNew}).
     */
    @Transactional
    public void registerAccount(RegisterAccountCommand command) {
        permissions.require(CallerContext.GUEST, PermissionMatrix.REGISTER_ACCOUNT);

        // E2 — password policy is enforced in the domain, never the controller.
        PasswordPolicy.firstViolation(command.password()).ifPresent(violation -> {
            throw IdentityErrors.passwordPolicyViolation(violation);
        });

        EmailAddress email = new EmailAddress(command.email());
        Account account = Account.register(UUID.randomUUID(), email,
            new CredentialHash(passwordEncoder.encode(command.password())), command.displayName(), clock);

        Account saved;
        try {
            saved = accounts.registerNew(account);
        } catch (AccountRepository.DuplicateEmailException e) {
            // E1 — BR-CUS-04: no account-existence disclosure. Same successful outcome as the
            // main scenario; only the event dispatched differs.
            events.publish(new DuplicateRegistrationAttempted(email.value(), Instant.now(clock)));
            return;
        }

        String verificationToken = verificationTokens.issue(saved.id());
        events.publish(new AccountRegistered(saved.id(), email.value(), verificationToken, Instant.now(clock)));
    }

    /**
     * E1 (expired) and E2 (unrecognised/already consumed) are reported identically as
     * {@code ECP-GEN-4040} — the caller cannot tell an expired link from a fabricated one, the
     * same non-disclosure reasoning `BR-CUS-04` already applies to duplicate registration.
     */
    @Transactional
    public void verifyEmailAddress(String rawToken) {
        permissions.require(CallerContext.GUEST, PermissionMatrix.VERIFY_EMAIL_ADDRESS);

        UUID accountId = verificationTokens.consume(rawToken)
            .orElseThrow(IdentityErrors::invalidVerificationLink);
        Account account = accounts.findById(accountId)
            .orElseThrow(IdentityErrors::invalidVerificationLink);

        if (account.pendingEmail() != null) {
            // UC-CUS-08 A1 completion: this token was issued for an email-change request, not the
            // initial registration — promote the pending address instead of re-verifying the account.
            account.confirmPendingEmail(clock);
        } else {
            account.verify(clock); // A1 — idempotent if already verified.
        }
        accounts.save(account);
        events.publishFrom(account);
    }

    /**
     * `UC-CUS-02` A2 — non-disclosive and rate-limited the same way as registration
     * (`BR-CUS-04`, `NFR-SEC-05`): the response never confirms whether the address exists.
     */
    @Transactional
    public void resendEmailVerification(String email) {
        permissions.require(CallerContext.GUEST, PermissionMatrix.RESEND_EMAIL_VERIFICATION);

        accounts.findByEmail(new EmailAddress(email))
            .filter(account -> !account.isVerified())
            .ifPresent(account -> {
                String rawToken = verificationTokens.issue(account.id());
                events.publish(new EmailVerificationResent(account.id(), account.email().value(), rawToken,
                    Instant.now(clock)));
            });
    }
}

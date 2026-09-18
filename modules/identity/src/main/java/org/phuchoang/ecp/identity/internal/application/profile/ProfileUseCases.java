package org.phuchoang.ecp.identity.internal.application.profile;

import org.phuchoang.ecp.identity.internal.application.IdentityErrors;
import org.phuchoang.ecp.identity.internal.application.port.DomainEventPublisher;
import org.phuchoang.ecp.identity.internal.application.registration.VerificationTokenManager;
import org.phuchoang.ecp.identity.internal.application.security.CallerContext;
import org.phuchoang.ecp.identity.internal.application.security.PermissionChecker;
import org.phuchoang.ecp.identity.internal.application.security.PermissionMatrix;
import org.phuchoang.ecp.identity.internal.domain.event.EmailVerificationResent;
import org.phuchoang.ecp.identity.internal.domain.model.Account;
import org.phuchoang.ecp.identity.internal.domain.model.EmailAddress;
import org.phuchoang.ecp.identity.internal.domain.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/** `UC-CUS-08` — Manage Profile (`US-CUS-08`): {@code getOwnAccount}, {@code updateOwnProfile}. */
@Service
public class ProfileUseCases {

    private final AccountRepository accounts;
    private final VerificationTokenManager verificationTokens;
    private final PermissionChecker permissions;
    private final DomainEventPublisher events;
    private final Clock clock;

    public ProfileUseCases(AccountRepository accounts, VerificationTokenManager verificationTokens,
            PermissionChecker permissions, DomainEventPublisher events, Clock clock) {
        this.accounts = accounts;
        this.verificationTokens = verificationTokens;
        this.permissions = permissions;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AccountSummary getOwnAccount(CallerContext caller) {
        permissions.require(caller, PermissionMatrix.GET_OWN_ACCOUNT);
        Account account = accounts.findById(caller.accountId()).orElseThrow(IdentityErrors::accountNotFound);
        return AccountSummary.of(account);
    }

    @Transactional
    public AccountSummary updateOwnProfile(CallerContext caller, UpdateProfileCommand command) {
        permissions.require(caller, PermissionMatrix.UPDATE_OWN_PROFILE);

        Account account = accounts.findById(caller.accountId()).orElseThrow(IdentityErrors::accountNotFound);

        // E2 — whole-or-nothing: validate every field before storing any of them.
        EmailAddress newEmail = requestedEmailChange(account, command.email());

        // A2 — no field actually changed: still confirms success, stores nothing further.
        account.updateDisplayName(command.displayName());

        if (newEmail != null) {
            account.requestEmailChange(newEmail, clock);
            String verificationToken = verificationTokens.issue(account.id());
            accounts.save(account);
            events.publishFrom(account);
            events.publish(new EmailVerificationResent(account.id(), newEmail.value(), verificationToken,
                Instant.now(clock)));
        } else {
            account.confirmProfileUpdate(clock);
            accounts.save(account);
            events.publishFrom(account);
        }

        return AccountSummary.of(account);
    }

    /** {@code null} when the email is absent from the command or unchanged (A2). */
    private EmailAddress requestedEmailChange(Account account, String requestedEmail) {
        if (requestedEmail == null) {
            return null;
        }
        EmailAddress newEmail = new EmailAddress(requestedEmail);
        if (newEmail.equals(account.email())) {
            return null;
        }
        // E1 — no existence disclosure: reported as "cannot be used", never "already registered".
        accounts.findByEmail(newEmail).ifPresent(existing -> {
            throw IdentityErrors.emailUnavailable();
        });
        return newEmail;
    }
}

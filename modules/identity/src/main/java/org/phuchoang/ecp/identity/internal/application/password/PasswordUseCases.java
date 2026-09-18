package org.phuchoang.ecp.identity.internal.application.password;

import org.phuchoang.ecp.identity.internal.application.IdentityErrors;
import org.phuchoang.ecp.identity.internal.application.authentication.SessionManager;
import org.phuchoang.ecp.identity.internal.application.password.PasswordResetTokenManager.PendingReset;
import org.phuchoang.ecp.identity.internal.application.port.DomainEventPublisher;
import org.phuchoang.ecp.identity.internal.application.port.PasswordEncoder;
import org.phuchoang.ecp.identity.internal.application.security.CallerContext;
import org.phuchoang.ecp.identity.internal.application.security.PermissionChecker;
import org.phuchoang.ecp.identity.internal.application.security.PermissionMatrix;
import org.phuchoang.ecp.identity.internal.domain.event.PasswordResetRequested;
import org.phuchoang.ecp.identity.internal.domain.model.Account;
import org.phuchoang.ecp.identity.internal.domain.model.CredentialHash;
import org.phuchoang.ecp.identity.internal.domain.model.EmailAddress;
import org.phuchoang.ecp.identity.internal.domain.model.PasswordPolicy;
import org.phuchoang.ecp.identity.internal.domain.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Passwords: `UC-CUS-06` Change Password and `UC-CUS-07` Reset Forgotten Password.
 *
 * <p>"End other sessions" (`endOtherSessions`, default {@code true}) is implemented as ending
 * every outstanding {@code REFRESH} token for the account — the server holds no session state to
 * single out "this caller's own refresh token" from a bearer access token alone (`ADR-0016` §4).
 *
 * <p>Reset mirrors registration's non-disclosure shape exactly (`BR-CUS-04`): the caller never
 * learns whether the address exists, and every invalid-link case is reported identically.
 */
@Service
public class PasswordUseCases {

    private final AccountRepository accounts;
    private final PasswordResetTokenManager resetTokens;
    private final SessionManager sessions;
    private final PasswordEncoder passwordEncoder;
    private final PermissionChecker permissions;
    private final DomainEventPublisher events;
    private final Clock clock;

    public PasswordUseCases(AccountRepository accounts, PasswordResetTokenManager resetTokens, SessionManager sessions,
            PasswordEncoder passwordEncoder, PermissionChecker permissions, DomainEventPublisher events, Clock clock) {
        this.accounts = accounts;
        this.resetTokens = resetTokens;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
        this.permissions = permissions;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public void changeOwnPassword(CallerContext caller, ChangePasswordCommand command) {
        permissions.require(caller, PermissionMatrix.CHANGE_OWN_PASSWORD);

        Account account = accounts.findById(caller.accountId()).orElseThrow(IdentityErrors::accountNotFound);

        // E1 — re-verification, notwithstanding the active session (a hijacked session must not be
        // able to seize the account by changing its password).
        if (!passwordEncoder.matches(command.currentPassword(), account.credentialHash().value())) {
            throw IdentityErrors.currentPasswordIncorrect();
        }
        // E3 — rotating to the same password serves no purpose.
        if (passwordEncoder.matches(command.newPassword(), account.credentialHash().value())) {
            throw IdentityErrors.passwordUnchanged();
        }
        // E2
        PasswordPolicy.firstViolation(command.newPassword()).ifPresent(violation -> {
            throw IdentityErrors.passwordPolicyViolation(violation);
        });

        account.changePassword(new CredentialHash(passwordEncoder.encode(command.newPassword())), clock);
        accounts.save(account);

        if (command.endOtherSessions()) {
            sessions.revokeAll(account.id());
        }
        events.publishFrom(account);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        permissions.require(CallerContext.GUEST, PermissionMatrix.REQUEST_PASSWORD_RESET);

        // E1 — the response is identical whether or not the address is registered; nothing is
        // dispatched and nothing is disclosed to the caller either way.
        accounts.findByEmail(new EmailAddress(email)).ifPresent(account -> {
            String rawToken = resetTokens.issue(account.id());
            events.publish(new PasswordResetRequested(account.id(), account.email().value(), rawToken,
                Instant.now(clock)));
        });
    }

    @Transactional
    public void completePasswordReset(String rawToken, String newPassword) {
        permissions.require(CallerContext.GUEST, PermissionMatrix.COMPLETE_PASSWORD_RESET);

        PendingReset reset = resetTokens.findUsable(rawToken).orElseThrow(IdentityErrors::invalidResetLink);

        // E3 — the token is not consumed on a policy failure, so the guest can retry without a new link.
        PasswordPolicy.firstViolation(newPassword).ifPresent(violation -> {
            throw IdentityErrors.passwordPolicyViolation(violation);
        });

        if (!resetTokens.consume(reset)) {
            throw IdentityErrors.invalidResetLink();
        }

        Account account = accounts.findById(reset.accountId()).orElseThrow(IdentityErrors::invalidResetLink);
        // Account closed: the token was already consumed above, so a retry cannot succeed either
        // — reported identically to an invalid link, the same non-disclosure reasoning as E1/E2.
        if (!account.isActive()) {
            throw IdentityErrors.invalidResetLink();
        }
        account.changePassword(new CredentialHash(passwordEncoder.encode(newPassword)), clock);
        account.verify(clock); // A1 — completing a reset proves control of the address.
        accounts.save(account);

        // BR-CUS-03 — every existing session ends, not just "other" ones (there is no active
        // session to preserve: the caller who completes a reset was, until now, logged out).
        sessions.revokeAll(account.id());
        events.publishFrom(account);
    }
}

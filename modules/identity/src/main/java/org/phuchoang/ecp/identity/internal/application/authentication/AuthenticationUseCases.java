package org.phuchoang.ecp.identity.internal.application.authentication;

import org.phuchoang.ecp.identity.internal.application.IdentityErrors;
import org.phuchoang.ecp.identity.internal.application.authentication.SessionManager.Presentation;
import org.phuchoang.ecp.identity.internal.application.port.AccessTokenIssuer;
import org.phuchoang.ecp.identity.internal.application.port.DomainEventPublisher;
import org.phuchoang.ecp.identity.internal.application.port.PasswordEncoder;
import org.phuchoang.ecp.identity.internal.application.profile.AccountSummary;
import org.phuchoang.ecp.identity.internal.application.security.CallerContext;
import org.phuchoang.ecp.identity.internal.application.security.PermissionChecker;
import org.phuchoang.ecp.identity.internal.application.security.PermissionMatrix;
import org.phuchoang.ecp.identity.internal.domain.event.SessionEnded;
import org.phuchoang.ecp.identity.internal.domain.event.SessionEstablished;
import org.phuchoang.ecp.identity.internal.domain.model.Account;
import org.phuchoang.ecp.identity.internal.domain.model.AccountStatus;
import org.phuchoang.ecp.identity.internal.domain.model.EmailAddress;
import org.phuchoang.ecp.identity.internal.domain.repository.AccountRepository;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Sessions: `UC-CUS-03` Log In, `UC-CUS-04` Log Out, `UC-CUS-05` Refresh Authenticated Session.
 *
 * <p>Log in: E1 (bad credentials) and E3 (suspended account) return the exact same
 * {@code ECP-GEN-4010} with the exact same message — `BR-CUS-04` requires the caller cannot
 * distinguish "no such account", "wrong password", and "suspended" from the response.
 *
 * <p>Log out: E1 (already ended / unrecognised token) is success-with-no-action, never an error —
 * the caller's goal (not being logged in) already holds, and an error here only invites a
 * useless retry.
 *
 * <p>Renewal: the caller has no valid access token at this point by definition, so unlike every
 * other use case this one cannot build its {@link CallerContext} from the token's claims. It
 * re-reads the account's <em>current</em> roles instead, which also means a role granted or
 * revoked since the last login takes effect on the very next refresh.
 */
@Service
public class AuthenticationUseCases {

    private final AccountRepository accounts;
    private final SessionManager sessions;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenIssuer accessTokens;
    private final PermissionChecker permissions;
    private final DomainEventPublisher events;
    private final Clock clock;

    public AuthenticationUseCases(AccountRepository accounts, SessionManager sessions, PasswordEncoder passwordEncoder,
            AccessTokenIssuer accessTokens, PermissionChecker permissions, DomainEventPublisher events, Clock clock) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
        this.accessTokens = accessTokens;
        this.permissions = permissions;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public LoginResult logIn(LoginCommand command) {
        permissions.require(CallerContext.GUEST, PermissionMatrix.LOG_IN);

        Account account = accounts.findByEmail(new EmailAddress(command.email()))
            .orElseThrow(IdentityErrors::invalidCredentials); // E1 — unknown address, same message as wrong password.

        if (!passwordEncoder.matches(command.password(), account.credentialHash().value())) {
            account.recordFailedLogin();
            accounts.save(account);
            throw IdentityErrors.invalidCredentials(); // E1.
        }
        if (account.status() != AccountStatus.ACTIVE) {
            throw IdentityErrors.invalidCredentials(); // E3 — suspended is reported exactly like a credential failure.
        }

        account.recordSuccessfulLogin(clock);
        accounts.save(account);

        boolean restricted = !account.isVerified(); // A1 — restricted session, browse/cart only.
        events.publish(new SessionEstablished(account.id(), restricted, Instant.now(clock)));

        return session(account, sessions.issue(account.id()));
    }

    @Transactional
    public void logOut(CallerContext caller, String rawRefreshToken) {
        permissions.require(caller, PermissionMatrix.LOG_OUT);

        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            sessions.revoke(rawRefreshToken); // Absent, expired, or already consumed — E1: success, no action.
        }
        events.publish(new SessionEnded(caller.accountId(), false, Instant.now(clock)));
    }

    @Transactional
    public void endAllOwnSessions(CallerContext caller) {
        permissions.require(caller, PermissionMatrix.END_ALL_OWN_SESSIONS);
        sessions.revokeAll(caller.accountId());
        events.publish(new SessionEnded(caller.accountId(), true, Instant.now(clock)));
    }

    // noRollbackFor: a reuse rejection's whole point is a durable side effect (the chain
    // invalidation) that must survive the DomainException it then throws to reject *this* call —
    // Spring's default rollback-on-RuntimeException would otherwise undo the very write reuse
    // detection depends on.
    @Transactional(noRollbackFor = DomainException.class)
    public LoginResult renewSession(String rawRefreshToken) {
        Presentation.Usable presented = switch (sessions.present(rawRefreshToken)) {
            case Presentation.Invalid invalid -> throw IdentityErrors.refreshTokenRejected();
            case Presentation.Reused reused -> throw chainEnded(reused.accountId());
            case Presentation.Usable usable -> usable;
        };

        Account account = accounts.findById(presented.accountId())
            .orElseThrow(IdentityErrors::refreshTokenRejected);
        permissions.require(new CallerContext(account.id(), account.roles()), PermissionMatrix.RENEW_SESSION);

        String rawNewRefreshToken = sessions.rotate(presented)
            .orElseThrow(() -> chainEnded(account.id()));

        return session(account, rawNewRefreshToken);
    }

    private DomainException chainEnded(java.util.UUID accountId) {
        events.publish(new SessionEnded(accountId, true, Instant.now(clock)));
        return IdentityErrors.refreshTokenReused();
    }

    private LoginResult session(Account account, String rawRefreshToken) {
        AccessTokenIssuer.IssuedAccessToken accessToken = accessTokens.issue(account.id(), account.roles());
        return new LoginResult(accessToken.token(), rawRefreshToken, accessToken.expiresInSeconds(),
            !account.isVerified(), AccountSummary.of(account));
    }
}

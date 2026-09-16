package org.phuchoang.ecp.identity.application.command;

import org.phuchoang.ecp.identity.application.CallerContext;
import org.phuchoang.ecp.identity.application.internal.PermissionMatrix;
import org.phuchoang.ecp.identity.application.port.AccountRepository;
import org.phuchoang.ecp.identity.application.port.AuthorizationService;
import org.phuchoang.ecp.identity.application.port.PasswordEncoder;
import org.phuchoang.ecp.identity.application.port.TokenRepository;
import org.phuchoang.ecp.identity.domain.Account;
import org.phuchoang.ecp.identity.domain.CredentialHash;
import org.phuchoang.ecp.identity.domain.PasswordPolicy;
import org.phuchoang.ecp.identity.domain.TokenType;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;
import org.phuchoang.ecp.sharedkernel.api.error.GenErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * `UC-CUS-06` — Change Password (`US-CUS-06`): {@code changeOwnPassword}.
 *
 * <p>The server holds no session state to identify "this caller's own refresh token" from a
 * bearer access token alone (`ADR-0016` §4 — `ecp-api` is always stateless bearer); the only
 * durable session record is the {@code REFRESH} token chain. So "end other sessions"
 * (`endOtherSessions`, default {@code true}) is implemented as ending every outstanding
 * {@code REFRESH} token for the account — the same mechanism `US-CUS-05`'s logout-everywhere
 * uses — rather than excluding one specific session the server has no identifier for.
 */
@Service
public class ChangePasswordService {

    private final AccountRepository accountRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthorizationService authorizationService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public ChangePasswordService(AccountRepository accountRepository, TokenRepository tokenRepository,
            PasswordEncoder passwordEncoder, AuthorizationService authorizationService,
            ApplicationEventPublisher events, Clock clock) {
        this.accountRepository = accountRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorizationService = authorizationService;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public void changeOwnPassword(CallerContext caller, ChangePasswordCommand command) {
        authorizationService.assertAuthorized(caller, PermissionMatrix.CHANGE_OWN_PASSWORD);

        Account account = accountRepository.findById(caller.accountId())
            .orElseThrow(() -> new DomainException(GenErrorCode.NOT_FOUND, "Account not found."));

        // E1 — re-verification, notwithstanding the active session (a hijacked session must not be
        // able to seize the account by changing its password).
        if (!passwordEncoder.matches(command.currentPassword(), account.credentialHash().value())) {
            throw new DomainException(GenErrorCode.VALIDATION_FAILED, "Current password is incorrect.");
        }

        // E3 — rotating to the same password serves no purpose.
        if (passwordEncoder.matches(command.newPassword(), account.credentialHash().value())) {
            throw new DomainException(GenErrorCode.VALIDATION_FAILED,
                "New password must be different from the current password.");
        }

        // E2
        PasswordPolicy.firstViolation(command.newPassword()).ifPresent(violation -> {
            throw new DomainException(GenErrorCode.VALIDATION_FAILED, violation);
        });

        account.changePassword(new CredentialHash(passwordEncoder.encode(command.newPassword())), clock);
        accountRepository.save(account);

        if (command.endOtherSessions()) {
            tokenRepository.invalidateOutstanding(caller.accountId(), TokenType.REFRESH);
        }

        account.pullDomainEvents().forEach(events::publishEvent);
    }
}

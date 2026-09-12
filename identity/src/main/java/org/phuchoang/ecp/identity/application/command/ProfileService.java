package org.phuchoang.ecp.identity.application.command;

import org.phuchoang.ecp.identity.application.CallerContext;
import org.phuchoang.ecp.identity.application.internal.PermissionMatrix;
import org.phuchoang.ecp.identity.application.internal.VerificationTokenIssuing;
import org.phuchoang.ecp.identity.application.mapper.AccountSummary;
import org.phuchoang.ecp.identity.application.port.AccountRepository;
import org.phuchoang.ecp.identity.application.port.AuthorizationService;
import org.phuchoang.ecp.identity.application.port.TokenRepository;
import org.phuchoang.ecp.identity.domain.Account;
import org.phuchoang.ecp.identity.domain.EmailAddress;
import org.phuchoang.ecp.sharedkernel.api.DomainException;
import org.phuchoang.ecp.sharedkernel.api.GenErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** `UC-CUS-08` — Manage Profile (`US-CUS-08`): {@code getOwnAccount}, {@code updateOwnProfile}. */
@Service
public class ProfileService {

    private final AccountRepository accountRepository;
    private final TokenRepository tokenRepository;
    private final AuthorizationService authorizationService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public ProfileService(AccountRepository accountRepository, TokenRepository tokenRepository,
            AuthorizationService authorizationService, ApplicationEventPublisher events, Clock clock) {
        this.accountRepository = accountRepository;
        this.tokenRepository = tokenRepository;
        this.authorizationService = authorizationService;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AccountSummary getOwnAccount(CallerContext caller) {
        authorizationService.assertAuthorized(caller, PermissionMatrix.GET_OWN_ACCOUNT);
        Account account = accountRepository.findById(caller.accountId())
            .orElseThrow(() -> new DomainException(GenErrorCode.NOT_FOUND, "Account not found."));
        return AccountSummary.of(account);
    }

    @Transactional
    public AccountSummary updateOwnProfile(CallerContext caller, UpdateProfileCommand command) {
        authorizationService.assertAuthorized(caller, PermissionMatrix.UPDATE_OWN_PROFILE);

        Account account = accountRepository.findById(caller.accountId())
            .orElseThrow(() -> new DomainException(GenErrorCode.NOT_FOUND, "Account not found."));

        // E2 — whole-or-nothing: validate every field before storing any of them.
        EmailAddress newEmail = null;
        if (command.email() != null) {
            newEmail = new EmailAddress(command.email());
            if (!newEmail.equals(account.email())) {
                // E1 — no existence disclosure: reported as "cannot be used", never "already registered".
                accountRepository.findByEmail(newEmail).ifPresent(existing -> {
                    throw new DomainException(GenErrorCode.VALIDATION_FAILED, "This email address cannot be used.");
                });
            } else {
                newEmail = null; // A2 — unchanged; nothing to hold pending.
            }
        }

        // A2 — no field actually changed: still confirms success, stores nothing further.
        account.updateDisplayName(command.displayName());

        if (newEmail != null) {
            account.requestEmailChange(newEmail, clock);
            String verificationToken = VerificationTokenIssuing.issue(tokenRepository, account.id(), clock);
            accountRepository.save(account);
            account.pullDomainEvents().forEach(events::publishEvent);
            events.publishEvent(new org.phuchoang.ecp.identity.domain.EmailVerificationResent(account.id(),
                newEmail.value(), verificationToken, java.time.Instant.now(clock)));
        } else {
            account.confirmProfileUpdate(clock);
            accountRepository.save(account);
            account.pullDomainEvents().forEach(events::publishEvent);
        }

        return AccountSummary.of(account);
    }
}

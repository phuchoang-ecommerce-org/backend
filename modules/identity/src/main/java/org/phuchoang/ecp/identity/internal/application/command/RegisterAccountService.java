package org.phuchoang.ecp.identity.internal.application.command;

import org.phuchoang.ecp.identity.internal.application.CallerContext;
import org.phuchoang.ecp.identity.internal.application.command.model.RegisterAccountCommand;
import org.phuchoang.ecp.identity.internal.application.internal.PermissionMatrix;
import org.phuchoang.ecp.identity.internal.application.internal.VerificationTokenIssuing;
import org.phuchoang.ecp.identity.internal.domain.repository.AccountRepository;
import org.phuchoang.ecp.identity.internal.application.port.AuthorizationService;
import org.phuchoang.ecp.identity.internal.application.port.PasswordEncoder;
import org.phuchoang.ecp.identity.internal.application.port.TokenStore;
import org.phuchoang.ecp.identity.internal.domain.model.Account;
import org.phuchoang.ecp.identity.internal.domain.event.AccountRegistered;
import org.phuchoang.ecp.identity.internal.domain.model.CredentialHash;
import org.phuchoang.ecp.identity.internal.domain.event.DuplicateRegistrationAttempted;
import org.phuchoang.ecp.identity.internal.domain.model.EmailAddress;
import org.phuchoang.ecp.identity.internal.domain.model.PasswordPolicy;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;
import org.phuchoang.ecp.sharedkernel.api.error.GenErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * `UC-CUS-01` — Register Customer Account (`US-CUS-01`). Duplicate email (E1)
 * and success both
 * return normally: {@code BR-CUS-04} requires the response to never disclose
 * which case occurred,
 * so the distinction is made only in which event is raised, never in the
 * outcome the controller sees.
 */
@Service
@Transactional
public class RegisterAccountService {

  private final AccountRepository accountRepository;
  private final TokenStore tokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthorizationService authorizationService;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public RegisterAccountService(AccountRepository accountRepository, TokenStore tokenRepository,
      PasswordEncoder passwordEncoder, AuthorizationService authorizationService,
      ApplicationEventPublisher events, Clock clock) {
    this.accountRepository = accountRepository;
    this.tokenRepository = tokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.authorizationService = authorizationService;
    this.events = events;
    this.clock = clock;
  }

  // Not @Transactional at this level: registerNew() below deliberately runs in
  // its own
  // REQUIRES_NEW transaction, since a duplicate-email failure must roll back only
  // the attempted
  // insert, never poison a surrounding one (see AccountRepository#registerNew).
  public void registerAccount(RegisterAccountCommand command) {
    authorizationService.assertAuthorized(CallerContext.GUEST, PermissionMatrix.REGISTER_ACCOUNT);

    // E2 — password policy is enforced in the domain, never the controller (Sprint
    // 03 backlog).
    PasswordPolicy.firstViolation(command.password()).ifPresent(violation -> {
      throw new DomainException(GenErrorCode.VALIDATION_FAILED, violation);
    });

    EmailAddress email = new EmailAddress(command.email());
    Account account = Account.register(UUID.randomUUID(), email,
        new CredentialHash(passwordEncoder.encode(command.password())), command.displayName(), clock);

    Account saved;
    try {
      saved = accountRepository.registerNew(account);
    } catch (AccountRepository.DuplicateEmailException e) {
      // E1 — BR-CUS-04: no account-existence disclosure. Same successful outcome as
      // the
      // main scenario; only the event dispatched differs (stubbed as a log line this
      // sprint — see Sprint 03 Review Notes).
      events.publishEvent(new DuplicateRegistrationAttempted(email.value(), Instant.now(clock)));
      return;
    }

    String verificationToken = VerificationTokenIssuing.issue(tokenRepository, saved.id(), clock);
    events.publishEvent(new AccountRegistered(saved.id(), email.value(), verificationToken, Instant.now(clock)));
  }
}

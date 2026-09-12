package org.phuchoang.ecp.identity.infrastructure.event;

import org.phuchoang.ecp.identity.domain.AccountRegistered;
import org.phuchoang.ecp.identity.domain.AccountVerified;
import org.phuchoang.ecp.identity.domain.DuplicateRegistrationAttempted;
import org.phuchoang.ecp.identity.domain.EmailVerificationResent;
import org.phuchoang.ecp.identity.domain.SessionEnded;
import org.phuchoang.ecp.identity.domain.SessionEstablished;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * <b>Sprint 03 scope decision</b> (see the sprint's Review Notes): the {@code notification} and
 * {@code audit} modules are still empty scaffolds — building either fully is not a committed
 * Sprint 03 item. These listeners prove the ADR-0012 §4 in-process event transport is wired
 * correctly (real {@code @ApplicationModuleListener}s, real {@code AFTER_COMMIT} semantics) by
 * logging in place of sending a real email or writing a real {@code audit_entry} row.
 *
 * <p>The verification token itself is deliberately never logged (`NFR-SEC-07`) even though the
 * event carries it for the (currently absent) real notification listener to build a link with.
 */
@Component
class NotificationAndAuditStubListeners {

    private static final Logger log = LoggerFactory.getLogger(NotificationAndAuditStubListeners.class);

    @ApplicationModuleListener
    void on(AccountRegistered event) {
        log.info("[stub-notification] verification email would be sent to {} (accountId={}, token redacted)",
            event.email(), event.accountId());
        log.info("[stub-audit] UC-AUD-01: registerAccount accountId={}", event.accountId());
    }

    @ApplicationModuleListener
    void on(DuplicateRegistrationAttempted event) {
        log.info("[stub-notification] \"someone tried to register with your address\" email would be "
            + "sent to {} (BR-CUS-04)", event.email());
    }

    @ApplicationModuleListener
    void on(EmailVerificationResent event) {
        log.info("[stub-notification] verification email would be resent to {} (accountId={}, token redacted)",
            event.email(), event.accountId());
    }

    @ApplicationModuleListener
    void on(AccountVerified event) {
        log.info("[stub-audit] UC-AUD-01: verifyEmailAddress accountId={}", event.accountId());
    }

    @ApplicationModuleListener
    void on(SessionEstablished event) {
        log.info("[stub-audit] UC-AUD-01: logIn accountId={} restricted={}", event.accountId(), event.restricted());
    }

    @ApplicationModuleListener
    void on(SessionEnded event) {
        log.info("[stub-audit] UC-AUD-01: logOut accountId={} allSessions={}", event.accountId(), event.allSessions());
    }
}

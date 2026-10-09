package org.phuchoang.ecp.identity.internal.infrastructure.event;

import org.phuchoang.ecp.identity.internal.domain.event.AccountRegistered;
import org.phuchoang.ecp.identity.internal.domain.event.DuplicateRegistrationAttempted;
import org.phuchoang.ecp.identity.internal.domain.event.EmailChangeRequested;
import org.phuchoang.ecp.identity.internal.domain.event.EmailVerificationResent;
import org.phuchoang.ecp.identity.internal.domain.event.PasswordChanged;
import org.phuchoang.ecp.identity.internal.domain.event.PasswordResetRequested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Notification remains a later-sprint delivery adapter; auditing no longer shares this stub. */
@Component
class NotificationStubListeners {
    private static final Logger log = LoggerFactory.getLogger(NotificationStubListeners.class);

    @ApplicationModuleListener
    void on(AccountRegistered event) {
        log.info("[stub-notification] verification email would be sent to {} (accountId={}, token redacted)",
            event.email(), event.accountId());
    }

    @ApplicationModuleListener
    void on(DuplicateRegistrationAttempted event) {
        log.info("[stub-notification] registration warning would be sent to {}", event.email());
    }

    @ApplicationModuleListener
    void on(EmailVerificationResent event) {
        log.info("[stub-notification] verification email would be resent to {} (accountId={}, token redacted)",
            event.email(), event.accountId());
    }

    @ApplicationModuleListener
    void on(PasswordChanged event) {
        log.info("[stub-notification] password-change notification would be sent for account {}", event.accountId());
    }

    @ApplicationModuleListener
    void on(PasswordResetRequested event) {
        log.info("[stub-notification] password-reset email would be sent to {} (accountId={}, token redacted)",
            event.email(), event.accountId());
    }

    @ApplicationModuleListener
    void on(EmailChangeRequested event) {
        log.info("[stub-notification] email-change notification would be sent for account {}", event.accountId());
    }
}

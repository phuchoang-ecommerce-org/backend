package org.phuchoang.ecp.identity.internal.application.event;

/** A use-case-owned in-process signal that is not a fact raised by an aggregate root. */
public sealed interface IdentityWorkflowEvent permits DuplicateRegistrationAttempted,
        VerificationEmailRequested, PasswordResetRequested, SessionEstablished, SessionEnded {
}

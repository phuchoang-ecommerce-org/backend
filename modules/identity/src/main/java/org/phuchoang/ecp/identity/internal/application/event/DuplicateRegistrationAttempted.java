package org.phuchoang.ecp.identity.internal.application.event;

import java.time.Instant;

public record DuplicateRegistrationAttempted(String email, Instant occurredAt) implements IdentityWorkflowEvent { }

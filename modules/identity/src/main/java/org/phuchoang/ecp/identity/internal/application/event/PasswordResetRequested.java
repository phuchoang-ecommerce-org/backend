package org.phuchoang.ecp.identity.internal.application.event;

import java.time.Instant;
import java.util.UUID;

public record PasswordResetRequested(UUID accountId, String email, String rawToken, Instant occurredAt)
        implements IdentityWorkflowEvent { }

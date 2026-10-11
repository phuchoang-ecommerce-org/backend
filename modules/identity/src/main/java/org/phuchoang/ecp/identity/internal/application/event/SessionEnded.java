package org.phuchoang.ecp.identity.internal.application.event;

import java.time.Instant;
import java.util.UUID;

public record SessionEnded(UUID accountId, boolean allSessions, Instant occurredAt) implements IdentityWorkflowEvent { }

package org.phuchoang.ecp.identity.internal.application.event;

import java.time.Instant;
import java.util.UUID;

public record SessionEstablished(UUID accountId, boolean restricted, Instant occurredAt) implements IdentityWorkflowEvent { }

package org.phuchoang.ecp.identity.internal.domain.event;

import org.jmolecules.event.annotation.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Raised on successful registration (`UC-CUS-01`). ADR-0012 §4 — in-process transport only, never
 * Kafka. Verification-token delivery is an application workflow concern, not part of this fact.
 */
@DomainEvent
public record AccountRegistered(UUID accountId, String email, Instant occurredAt) {
}

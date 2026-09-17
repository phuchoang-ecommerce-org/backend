package org.phuchoang.ecp.identity.internal.domain.event;

import org.jmolecules.event.annotation.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/** Raised when an account's email address is proven (`UC-CUS-02`). */
@DomainEvent
public record AccountVerified(UUID accountId, String email, Instant occurredAt) {
}

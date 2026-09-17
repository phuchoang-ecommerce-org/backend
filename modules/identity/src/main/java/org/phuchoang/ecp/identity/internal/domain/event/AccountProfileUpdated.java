package org.phuchoang.ecp.identity.internal.domain.event;

import org.jmolecules.event.annotation.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/** Raised whenever `updateOwnProfile` stores a change (`UC-CUS-08`) — the `UC-AUD-01` audit trigger. */
@DomainEvent
public record AccountProfileUpdated(UUID accountId, Instant occurredAt) {
}

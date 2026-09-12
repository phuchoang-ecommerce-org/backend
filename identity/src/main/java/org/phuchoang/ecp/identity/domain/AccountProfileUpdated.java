package org.phuchoang.ecp.identity.domain;

import java.time.Instant;
import java.util.UUID;

/** Raised whenever `updateOwnProfile` stores a change (`UC-CUS-08`) — the `UC-AUD-01` audit trigger. */
public record AccountProfileUpdated(UUID accountId, Instant occurredAt) {
}

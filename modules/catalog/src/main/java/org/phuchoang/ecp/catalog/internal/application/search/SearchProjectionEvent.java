package org.phuchoang.ecp.catalog.internal.application.search;

import java.time.Instant;
import java.util.UUID;

/** Internal projection input. Payload decoding remains local to the projection adapter. */
public record SearchProjectionEvent(UUID eventId, String eventType, Instant occurredAt, String aggregateType,
        UUID aggregateId, UUID correlationId, String payload) {
}

package org.phuchoang.ecp.catalog.api.search;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Transport-neutral subset of the v1 event envelope required by Catalog's search projector.
 * Payload decoding is owned by the search infrastructure adapter, not by this application API.
 */
public record CatalogSearchProjectionEvent(UUID eventId, String eventType, Instant occurredAt, String aggregateType,
        UUID aggregateId, UUID correlationId, String payload) {

    public CatalogSearchProjectionEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(aggregateId, "aggregateId");
        Objects.requireNonNull(correlationId, "correlationId");
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("payload must not be blank.");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType must not be blank.");
        }
        if (aggregateType == null || aggregateType.isBlank()) {
            throw new IllegalArgumentException("aggregateType must not be blank.");
        }
    }
}

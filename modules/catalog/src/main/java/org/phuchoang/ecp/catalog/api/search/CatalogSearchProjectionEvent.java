package org.phuchoang.ecp.catalog.api.search;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Transport-neutral subset of the v1 Kafka envelope required by Catalog's search projector. */
public record CatalogSearchProjectionEvent(UUID eventId, String eventType, Instant occurredAt, String aggregateType,
        UUID aggregateId, UUID correlationId, JsonNode payload) {

    public CatalogSearchProjectionEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(aggregateId, "aggregateId");
        Objects.requireNonNull(correlationId, "correlationId");
        Objects.requireNonNull(payload, "payload");
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType must not be blank.");
        }
        if (aggregateType == null || aggregateType.isBlank()) {
            throw new IllegalArgumentException("aggregateType must not be blank.");
        }
    }
}

package org.phuchoang.ecp.sharedkernel.api.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable, transport-neutral identity and causation metadata for an outbox event. */
public record EventMetadata(UUID eventId, String eventType, int eventVersion, Instant occurredAt,
        String aggregateType, UUID aggregateId, UUID correlationId, EventActor actor) {

    public EventMetadata {
        Objects.requireNonNull(eventId, "eventId");
        requireText(eventType, "eventType");
        if (eventVersion < 1) {
            throw new IllegalArgumentException("An event version must be at least one.");
        }
        Objects.requireNonNull(occurredAt, "occurredAt");
        requireText(aggregateType, "aggregateType");
        Objects.requireNonNull(aggregateId, "aggregateId");
        Objects.requireNonNull(correlationId, "correlationId");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank.");
        }
    }
}

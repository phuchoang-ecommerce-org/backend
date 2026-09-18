package org.phuchoang.ecp.messaging;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * The typed v1 event envelope (`04-shared/Event Contract/envelope.v1.json`) — the one Java model
 * both the outbox relay (producer side) and every Kafka listener (consumer side) speak. Every
 * property is always written, {@code actor} explicitly as {@code null} for automated work, so the
 * wire shape does not depend on the application's default inclusion policy. The payload stays a
 * tree: typed payload records are introduced per event contract where they pay for themselves.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record EventEnvelope(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String aggregateType,
        UUID aggregateId,
        UUID correlationId,
        Actor actor,
        JsonNode payload) {

    /** Attribution copied into the envelope; {@code null} at envelope level for automated work. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Actor(UUID userId, String role) {
    }
}

package org.phuchoang.ecp.sharedkernel.api.event;

import java.util.Objects;

/** A durable event request. Payload is the event-specific JSON object, never a Kafka client type. */
public record OutboxEvent(EventMetadata metadata, String topic, String payload) {

    public OutboxEvent {
        Objects.requireNonNull(metadata, "metadata");
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("An outbox topic must not be blank.");
        }
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("An outbox payload must not be blank.");
        }
    }
}

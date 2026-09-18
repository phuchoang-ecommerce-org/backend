package org.phuchoang.ecp.messaging;

import org.phuchoang.ecp.messaging.outbox.OutboxRecord;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * The single (de)serialisation mechanism for event envelopes: the application's configured
 * {@link ObjectMapper}, in both directions. No hand-built JSON anywhere in the relay or listeners.
 */
@Component
public class EventEnvelopeCodec {

    private final ObjectMapper json;

    public EventEnvelopeCodec(ObjectMapper json) {
        this.json = json;
    }

    public String serialize(EventEnvelope envelope) {
        return json.writeValueAsString(envelope);
    }

    public EventEnvelope deserialize(String body) {
        return json.readValue(body, EventEnvelope.class);
    }

    /** Lifts the stored outbox columns into the envelope; the JSONB payload text becomes the payload tree. */
    public EventEnvelope fromOutbox(OutboxRecord record) {
        EventEnvelope.Actor actor = record.actorUserId() == null
            ? null
            : new EventEnvelope.Actor(record.actorUserId(), record.actorRole());
        return new EventEnvelope(record.eventId(), record.eventType(), record.eventVersion(), record.occurredAt(),
            record.aggregateType(), record.aggregateId(), record.correlationId(), actor, json.readTree(record.payload()));
    }
}

package org.phuchoang.ecp.events;

/** Serialises the stored columns into the canonical UTF-8 JSON wire envelope. */
final class EventEnvelopeJson {

    private EventEnvelopeJson() {
    }

    static String serialize(OutboxRecord event) {
        String actor = event.actorUserId() == null ? "null" : "{\"userId\":\"" + event.actorUserId()
            + "\",\"role\":" + stringOrNull(event.actorRole()) + "}";
        return "{\"eventId\":\"" + event.eventId() + "\",\"eventType\":\"" + escape(event.eventType())
            + "\",\"eventVersion\":" + event.eventVersion() + ",\"occurredAt\":\"" + event.occurredAt()
            + "\",\"aggregateType\":\"" + escape(event.aggregateType()) + "\",\"aggregateId\":\""
            + event.aggregateId() + "\",\"correlationId\":\"" + event.correlationId() + "\",\"actor\":" + actor
            + ",\"payload\":" + event.payload() + "}";
    }

    private static String stringOrNull(String value) {
        return value == null ? "null" : "\"" + escape(value) + "\"";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\b", "\\b").replace("\f", "\\f").replace("\n", "\\n")
            .replace("\r", "\\r").replace("\t", "\\t");
    }
}

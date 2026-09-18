package org.phuchoang.ecp.messaging.outbox;

import java.time.Instant;
import java.util.UUID;

/** One stored outbox row, as the relay reads it; the payload is the JSONB column's text. */
public record OutboxRecord(long sequenceNo, UUID eventId, String eventType, int eventVersion, Instant occurredAt,
        String aggregateType, UUID aggregateId, UUID correlationId, UUID actorUserId, String actorRole,
        String payload, String topic) {
}

package org.phuchoang.ecp.events;

import java.time.Instant;
import java.util.UUID;

/** Database representation used only by the composition-root relay. */
record OutboxRecord(long sequenceNo, UUID eventId, String eventType, int eventVersion, Instant occurredAt,
        String aggregateType, UUID aggregateId, UUID correlationId, UUID actorUserId, String actorRole,
        String payload, String topic) {
}

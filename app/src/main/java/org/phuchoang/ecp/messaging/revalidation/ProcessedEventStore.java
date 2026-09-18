package org.phuchoang.ecp.messaging.revalidation;

import java.util.UUID;

/** Consumer-side idempotency: at-least-once delivery means every event may arrive more than once. */
public interface ProcessedEventStore {

    /** @return {@code true} if this call claimed the event; {@code false} if it was already processed */
    boolean tryMarkProcessed(UUID eventId, String eventType);
}

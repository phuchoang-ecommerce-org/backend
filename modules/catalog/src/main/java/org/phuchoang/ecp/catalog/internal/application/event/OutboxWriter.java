package org.phuchoang.ecp.catalog.internal.application.event;

/**
 * A bounded context's transactional port for recording an event in its own outbox table.
 * Implementations belong to the owning context; Kafka is intentionally not part of this contract.
 */
public interface OutboxWriter {

    void append(OutboxEvent event);
}


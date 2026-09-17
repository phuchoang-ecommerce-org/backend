package org.phuchoang.ecp.catalog.internal.infrastructure.persistence.write.outbox;

import org.phuchoang.ecp.sharedkernel.api.event.EventActor;
import org.phuchoang.ecp.sharedkernel.api.event.OutboxEvent;
import org.phuchoang.ecp.sharedkernel.api.event.OutboxWriter;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Catalog's implementation of the shared outbox port, restricted to {@code catalog_outbox}. */
@Repository
public class JdbcCatalogOutboxWriter implements OutboxWriter {

    private final JdbcClient jdbc;

    public JdbcCatalogOutboxWriter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void append(OutboxEvent event) {
        EventActor actor = event.metadata().actor();
        jdbc.sql("""
            INSERT INTO catalog_outbox (event_id, event_type, event_version, occurred_at, aggregate_type,
                aggregate_id, correlation_id, actor_user_id, actor_role, payload, topic)
            VALUES (:eventId, :eventType, :eventVersion, :occurredAt, :aggregateType, :aggregateId,
                :correlationId, :actorUserId, :actorRole, CAST(:payload AS jsonb), :topic)
            """)
            .param("eventId", event.metadata().eventId())
            .param("eventType", event.metadata().eventType())
            .param("eventVersion", event.metadata().eventVersion())
            .param("occurredAt", event.metadata().occurredAt())
            .param("aggregateType", event.metadata().aggregateType())
            .param("aggregateId", event.metadata().aggregateId())
            .param("correlationId", event.metadata().correlationId())
            .param("actorUserId", actor == null ? null : actor.userId())
            .param("actorRole", actor == null ? null : actor.role())
            .param("payload", event.payload())
            .param("topic", event.topic())
            .update();
    }
}

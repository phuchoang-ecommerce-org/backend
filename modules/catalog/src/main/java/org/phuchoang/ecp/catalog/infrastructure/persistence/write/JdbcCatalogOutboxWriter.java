package org.phuchoang.ecp.catalog.infrastructure.persistence.write;

import org.phuchoang.ecp.sharedkernel.api.event.EventActor;
import org.phuchoang.ecp.sharedkernel.api.event.OutboxEvent;
import org.phuchoang.ecp.sharedkernel.api.event.OutboxWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Catalog's implementation of the shared outbox port, restricted to {@code catalog_outbox}. */
@Repository
public class JdbcCatalogOutboxWriter implements OutboxWriter {

    private final JdbcTemplate jdbc;

    public JdbcCatalogOutboxWriter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void append(OutboxEvent event) {
        EventActor actor = event.metadata().actor();
        jdbc.update("""
            INSERT INTO catalog_outbox (event_id, event_type, event_version, occurred_at, aggregate_type,
                aggregate_id, correlation_id, actor_user_id, actor_role, payload, topic)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?)
            """, event.metadata().eventId(), event.metadata().eventType(), event.metadata().eventVersion(),
            event.metadata().occurredAt(), event.metadata().aggregateType(), event.metadata().aggregateId(),
            event.metadata().correlationId(), actor == null ? null : actor.userId(), actor == null ? null : actor.role(),
            event.payload(), event.topic());
    }
}

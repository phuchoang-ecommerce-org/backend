package org.phuchoang.ecp.messaging.revalidation;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/** {@link ProcessedEventStore} over {@code catalog_processed_event}; the primary key decides the race. */
@Repository
class JdbcProcessedEventStore implements ProcessedEventStore {

    private final JdbcClient jdbc;

    JdbcProcessedEventStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean tryMarkProcessed(UUID eventId, String eventType) {
        int claimed = jdbc.sql("""
            INSERT INTO catalog_processed_event (event_id, event_type) VALUES (:eventId, :eventType)
            ON CONFLICT DO NOTHING
            """).param("eventId", eventId).param("eventType", eventType).update();
        return claimed == 1;
    }
}

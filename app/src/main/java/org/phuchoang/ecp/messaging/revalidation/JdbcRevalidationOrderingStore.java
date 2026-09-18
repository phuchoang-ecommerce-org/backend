package org.phuchoang.ecp.messaging.revalidation;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/** {@link RevalidationOrderingStore} over {@code catalog_revalidation_cursor}; one conditional upsert per event. */
@Repository
class JdbcRevalidationOrderingStore implements RevalidationOrderingStore {

    private final JdbcClient jdbc;

    JdbcRevalidationOrderingStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean advanceIfNewer(UUID aggregateId, Instant occurredAt) {
        int advanced = jdbc.sql("""
            INSERT INTO catalog_revalidation_cursor (aggregate_id, occurred_at) VALUES (:aggregateId, :occurredAt)
            ON CONFLICT (aggregate_id) DO UPDATE SET occurred_at = EXCLUDED.occurred_at
            WHERE catalog_revalidation_cursor.occurred_at < EXCLUDED.occurred_at
            """)
            .param("aggregateId", aggregateId)
            .param("occurredAt", OffsetDateTime.ofInstant(occurredAt, ZoneOffset.UTC))
            .update();
        return advanced == 1;
    }
}

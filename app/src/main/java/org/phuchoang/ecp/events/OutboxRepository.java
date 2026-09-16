package org.phuchoang.ecp.events;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Read/mark operations for the relay. Table names are enum-owned constants, never user input. */
@Repository
class OutboxRepository {

    private final JdbcTemplate jdbc;

    OutboxRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<OutboxRecord> unpublished(OutboxModule module, int batchSize) {
        return jdbc.query("""
            SELECT sequence_no, event_id, event_type, event_version, occurred_at, aggregate_type, aggregate_id,
                   correlation_id, actor_user_id, actor_role, payload::text, topic
            FROM %s WHERE published_at IS NULL ORDER BY sequence_no LIMIT ?
            """.formatted(module.table()), this::row, batchSize);
    }

    void markPublished(OutboxModule module, OutboxRecord event) {
        jdbc.update("UPDATE %s SET published_at = now(), last_error = NULL WHERE event_id = ? AND published_at IS NULL"
            .formatted(module.table()), event.eventId());
    }

    void recordPublishFailure(OutboxModule module, OutboxRecord event, Exception failure) {
        String message = failure.getClass().getSimpleName() + ": " + String.valueOf(failure.getMessage());
        jdbc.update("UPDATE %s SET attempt_count = attempt_count + 1, last_error = ? WHERE event_id = ? "
            .formatted(module.table()), truncate(message), event.eventId());
    }

    double unpublishedDepth(OutboxModule module) {
        Long value = jdbc.queryForObject("SELECT count(*) FROM %s WHERE published_at IS NULL".formatted(module.table()),
            Long.class);
        return value == null ? 0 : value;
    }

    double oldestUnpublishedLagSeconds(OutboxModule module) {
        Double value = jdbc.queryForObject("SELECT COALESCE(EXTRACT(EPOCH FROM now() - min(occurred_at)), 0) "
            + "FROM %s WHERE published_at IS NULL".formatted(module.table()), Double.class);
        return value == null ? 0 : value;
    }

    private OutboxRecord row(ResultSet rs, int ignored) throws SQLException {
        return new OutboxRecord(rs.getLong("sequence_no"), rs.getObject("event_id", java.util.UUID.class),
            rs.getString("event_type"), rs.getInt("event_version"), rs.getTimestamp("occurred_at").toInstant(),
            rs.getString("aggregate_type"), rs.getObject("aggregate_id", java.util.UUID.class),
            rs.getObject("correlation_id", java.util.UUID.class), rs.getObject("actor_user_id", java.util.UUID.class),
            rs.getString("actor_role"), rs.getString("payload"), rs.getString("topic"));
    }

    private static String truncate(String value) {
        return value.length() <= 2_000 ? value : value.substring(0, 2_000);
    }
}

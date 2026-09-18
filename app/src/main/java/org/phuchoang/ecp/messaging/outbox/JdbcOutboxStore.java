package org.phuchoang.ecp.messaging.outbox;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * {@link OutboxStore} over the per-module {@code *_outbox} tables. This is the one class allowed
 * to use {@link JdbcTemplate} (ADR-0010 persistence-tool matrix): the relay's batch/mark
 * primitives are plain positional SQL over enum-owned table names.
 */
@Repository
class JdbcOutboxStore implements OutboxStore {

    private static final int MAX_ERROR_LENGTH = 2_000;

    private final JdbcTemplate jdbc;

    JdbcOutboxStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<OutboxRecord> unpublished(OutboxModule module, int limit) {
        return jdbc.query("""
            SELECT sequence_no, event_id, event_type, event_version, occurred_at, aggregate_type, aggregate_id,
                   correlation_id, actor_user_id, actor_role, payload::text, topic
            FROM %s WHERE published_at IS NULL ORDER BY sequence_no LIMIT ?
            """.formatted(module.table()), this::row, limit);
    }

    @Override
    public void markPublished(OutboxModule module, OutboxRecord record) {
        jdbc.update("UPDATE %s SET published_at = now(), last_error = NULL WHERE event_id = ? AND published_at IS NULL"
            .formatted(module.table()), record.eventId());
    }

    @Override
    public void recordPublishFailure(OutboxModule module, OutboxRecord record, Exception failure) {
        String message = failure.getClass().getSimpleName() + ": " + failure.getMessage();
        jdbc.update("UPDATE %s SET attempt_count = attempt_count + 1, last_error = ? WHERE event_id = ?"
            .formatted(module.table()), truncate(message), record.eventId());
    }

    @Override
    public double unpublishedDepth(OutboxModule module) {
        Long value = jdbc.queryForObject("SELECT count(*) FROM %s WHERE published_at IS NULL".formatted(module.table()),
            Long.class);
        return value == null ? 0 : value;
    }

    @Override
    public double oldestUnpublishedLagSeconds(OutboxModule module) {
        Double value = jdbc.queryForObject("SELECT COALESCE(EXTRACT(EPOCH FROM now() - min(occurred_at)), 0) "
            + "FROM %s WHERE published_at IS NULL".formatted(module.table()), Double.class);
        return value == null ? 0 : value;
    }

    private OutboxRecord row(ResultSet rs, int ignored) throws SQLException {
        return new OutboxRecord(rs.getLong("sequence_no"), rs.getObject("event_id", UUID.class),
            rs.getString("event_type"), rs.getInt("event_version"), rs.getTimestamp("occurred_at").toInstant(),
            rs.getString("aggregate_type"), rs.getObject("aggregate_id", UUID.class),
            rs.getObject("correlation_id", UUID.class), rs.getObject("actor_user_id", UUID.class),
            rs.getString("actor_role"), rs.getString("payload"), rs.getString("topic"));
    }

    private static String truncate(String value) {
        return value.length() <= MAX_ERROR_LENGTH ? value : value.substring(0, MAX_ERROR_LENGTH);
    }
}

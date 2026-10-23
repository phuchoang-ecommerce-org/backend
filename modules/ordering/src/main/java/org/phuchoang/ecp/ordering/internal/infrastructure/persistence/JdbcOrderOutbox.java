package org.phuchoang.ecp.ordering.internal.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.ordering.internal.application.place.command.OrderOutbox;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * Ordering's transactional outbox writer. The shared relay owns broker
 * publication and retries.
 */
@Repository
@RequiredArgsConstructor
class JdbcOrderOutbox implements OrderOutbox {
    private final JdbcClient jdbc;

    @Override
    public void appendOrderCreated(UUID orderId, UUID correlationId, IdentityActor actor, Instant occurredAt,
            String payload) {
        jdbc.sql(
                """
                        INSERT INTO ordering_outbox (event_id, event_type, event_version, occurred_at, aggregate_type, aggregate_id,
                            correlation_id, actor_user_id, actor_role, payload, topic)
                        VALUES (:eventId, 'OrderCreated', 1, :occurredAt, 'Order', :orderId, :correlationId, :actorUserId,
                            :actorRole, CAST(:payload AS jsonb), 'ecp.ordering.order.v1')
                        """)
                .param("eventId", UUID.randomUUID()).param("occurredAt", Timestamp.from(occurredAt))
                .param("orderId", orderId)
                .param("correlationId", correlationId).param("actorUserId", actor.accountId())
                .param("actorRole", actor.roles().stream().sorted().findFirst().orElse(null)).param("payload", payload)
                .update();
    }
}

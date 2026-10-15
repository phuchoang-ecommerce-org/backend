package org.phuchoang.ecp.ordering.internal.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.ordering.internal.application.place.command.OrderIdempotencyStore;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** PostgreSQL unique-key claim for BR-ORD-03; ON CONFLICT avoids poisoning the enclosing transaction. */
@Repository
@RequiredArgsConstructor
class JdbcOrderIdempotencyStore implements OrderIdempotencyStore {
    private final JdbcClient jdbc;

    @Override public Optional<Entry> find(String key) {
        return jdbc.sql("SELECT customer_id, request_hash, order_id FROM ordering_idempotency_key WHERE idempotency_key = :key")
            .param("key", key).query((rs, row) -> new Entry(rs.getObject("customer_id", UUID.class),
                rs.getString("request_hash"), rs.getObject("order_id", UUID.class))).optional();
    }

    @Override public boolean claim(String key, UUID customerId, String hash, UUID orderId) {
        return jdbc.sql("""
            INSERT INTO ordering_idempotency_key (idempotency_key, customer_id, request_hash, order_id)
            VALUES (:key, :customerId, :hash, :orderId) ON CONFLICT (idempotency_key) DO NOTHING
            """).param("key", key).param("customerId", customerId).param("hash", hash).param("orderId", orderId)
            .update() == 1;
    }
}

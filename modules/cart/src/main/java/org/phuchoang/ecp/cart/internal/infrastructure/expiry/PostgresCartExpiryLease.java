package org.phuchoang.ecp.cart.internal.infrastructure.expiry;

import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/** PostgreSQL session advisory lease, matching the outbox relay's multi-instance coordination pattern. */
@Component
class PostgresCartExpiryLease implements CartExpiryLease {
    private static final String KEY = "ecp.cart.expiry";
    private final DataSource dataSource;

    PostgresCartExpiryLease(DataSource dataSource) { this.dataSource = dataSource; }

    @Override
    public boolean executeIfAcquired(Runnable operation) {
        try (Connection connection = dataSource.getConnection()) {
            if (!tryLock(connection)) return false;
            try { operation.run(); } finally { unlock(connection); }
            return true;
        } catch (Exception exception) {
            throw new IllegalStateException("Cart expiry lease failed", exception);
        }
    }

    private static boolean tryLock(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_try_advisory_lock(hashtext(?))")) {
            statement.setString(1, KEY);
            try (ResultSet result = statement.executeQuery()) { result.next(); return result.getBoolean(1); }
        }
    }

    private static void unlock(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_unlock(hashtext(?))")) {
            statement.setString(1, KEY); statement.execute();
        }
    }
}

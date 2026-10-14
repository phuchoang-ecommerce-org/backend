package org.phuchoang.ecp.promotion.internal.infrastructure.lifecycle;

import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/** Session advisory lease, preventing two application replicas from firing one lifecycle transition twice. */
@Component
@lombok.RequiredArgsConstructor(access = lombok.AccessLevel.PACKAGE)
class PostgresPromotionLifecycleLease implements PromotionLifecycleLease {
    private static final String KEY = "ecp.promotion.lifecycle";
    private final DataSource dataSource;

    @Override public boolean executeIfAcquired(Runnable operation) {
        try (Connection connection = dataSource.getConnection()) {
            if (!tryLock(connection)) return false;
            try { operation.run(); } finally { unlock(connection); }
            return true;
        } catch (Exception exception) {
            throw new IllegalStateException("Promotion lifecycle lease failed", exception);
        }
    }

    private static boolean tryLock(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_try_advisory_lock(hashtext(?))")) {
            statement.setString(1, KEY);
            try (ResultSet results = statement.executeQuery()) { results.next(); return results.getBoolean(1); }
        }
    }

    private static void unlock(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_unlock(hashtext(?))")) {
            statement.setString(1, KEY); statement.execute();
        }
    }
}

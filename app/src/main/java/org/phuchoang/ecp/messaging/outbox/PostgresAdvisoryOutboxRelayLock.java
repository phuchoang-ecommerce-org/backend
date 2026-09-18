package org.phuchoang.ecp.messaging.outbox;

import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link OutboxRelayLock} on a PostgreSQL <em>session</em> advisory lock: held on a dedicated
 * connection for the duration of the operation and released in {@code finally}, so a crashed
 * replica's lock disappears with its session.
 */
@Component
class PostgresAdvisoryOutboxRelayLock implements OutboxRelayLock {

    private final DataSource dataSource;

    PostgresAdvisoryOutboxRelayLock(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public boolean executeIfAcquired(OutboxModule module, Runnable operation) {
        try (Connection connection = dataSource.getConnection()) {
            if (!tryLock(connection, module)) {
                return false;
            }
            try {
                operation.run();
            } finally {
                unlock(connection, module);
            }
            return true;
        } catch (SQLException exception) {
            throw new IllegalStateException("Outbox relay lock for module " + module.nameValue() + " failed", exception);
        }
    }

    private static boolean tryLock(Connection connection, OutboxModule module) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_try_advisory_lock(hashtext(?))")) {
            statement.setString(1, module.lockKey());
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getBoolean(1);
            }
        }
    }

    private static void unlock(Connection connection, OutboxModule module) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_unlock(hashtext(?))")) {
            statement.setString(1, module.lockKey());
            statement.execute();
        }
    }
}

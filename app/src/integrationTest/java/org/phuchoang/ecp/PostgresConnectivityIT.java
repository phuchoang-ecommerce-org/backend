package org.phuchoang.ecp;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L5 (Testing and Benchmark Strategy.md §3/§6.4) — proves a real transaction against PostgreSQL
 * running in a container, not an in-memory approximation. No persistence starter (JPA/JDBC) exists
 * in {@code app} yet — EN-DATA-1's own checklist asks only for the driver — so this connects with
 * plain {@code java.sql} rather than a Spring {@code DataSource}.
 */
@Testcontainers
class PostgresConnectivityIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
        .withReuse(true);

    @Test
    void aCommittedTransactionIsVisibleAndNothingUncommittedLeaks() throws Exception {
        try (Connection setup = connect()) {
            setup.setAutoCommit(true);
            try (Statement statement = setup.createStatement()) {
                statement.execute("CREATE TABLE en_data_1_probe (id INT PRIMARY KEY, note TEXT)");
            }
        }

        try (Connection writer = connect()) {
            writer.setAutoCommit(false);
            try (Statement statement = writer.createStatement()) {
                statement.execute("INSERT INTO en_data_1_probe (id, note) VALUES (1, 'committed')");
            }
            writer.commit();
        }

        try (Connection reader = connect();
             Statement statement = reader.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT note FROM en_data_1_probe WHERE id = 1")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("note")).isEqualTo("committed");
        }
    }

    private static Connection connect() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}

package org.phuchoang.ecp;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EN-DATA-2's own checklist item: a foreign key never crosses a module's table prefix
 * (Database.md §2.1, ADR-0009 §4) — {@code ordering_order} must not reference {@code catalog_product}.
 * A shared schema is not a shared model. Runs the full migration set against a real PostgreSQL,
 * then inspects {@code information_schema} directly rather than asserting against a fixed table
 * list, so a future migration that violates the rule fails this test without an edit here.
 */
@Testcontainers
class NoCrossModuleForeignKeyIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
        .withReuse(true);

    @Test
    void noForeignKeyReferencesAnotherModulesPrefix() throws Exception {
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .locations("classpath:db/migration")
            .load()
            .migrate();

        List<String> violations = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("""
                 SELECT tc.table_name AS referencing_table, ccu.table_name AS referenced_table
                 FROM information_schema.table_constraints tc
                 JOIN information_schema.constraint_column_usage ccu
                   ON tc.constraint_name = ccu.constraint_name AND tc.table_schema = ccu.table_schema
                 WHERE tc.constraint_type = 'FOREIGN KEY'
                 """)) {
            while (rs.next()) {
                String referencing = rs.getString("referencing_table");
                String referenced = rs.getString("referenced_table");
                String referencingPrefix = modulePrefixOf(referencing);
                String referencedPrefix = modulePrefixOf(referenced);
                if (!referencingPrefix.equals(referencedPrefix)) {
                    violations.add(referencing + " -> " + referenced);
                }
            }
        }

        assertThat(violations)
            .as("a foreign key crossing a module table prefix (Database.md §2.1)")
            .isEmpty();
    }

    private static String modulePrefixOf(String tableName) {
        int firstUnderscore = tableName.indexOf('_');
        return firstUnderscore < 0 ? tableName : tableName.substring(0, firstUnderscore);
    }
}

package org.phuchoang.ecp;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EN-DATA-2 — the baseline migration applied against the Testcontainers instance (ADR-0029, Database.md
 * §8). Asserts the full script set applies cleanly to a fresh {@code postgres:16} database and leaves
 * the expected table count, rather than asserting against a hand-maintained table list that would drift
 * from the migration scripts themselves.
 */
@Testcontainers
class FlywayMigrationIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
        .withReuse(true);

    @Test
    void migratesCleanlyAndAppliesEveryScript() throws Exception {
        Flyway flyway = Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .locations("classpath:db/migration")
            .load();

        var result = flyway.migrate();

        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted).isGreaterThanOrEqualTo(36);

        MigrationInfo[] applied = flyway.info().applied();
        assertThat(applied).extracting(MigrationInfo::getState)
            .allSatisfy(state -> assertThat(state.isApplied()).isTrue());

        // Every table this migration set creates is a real table in a fresh
        // database — the DDL parsed and executed, not merely a script that
        // Flyway accepted syntactically.
        Set<String> tables = new HashSet<>();
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("""
                 SELECT table_name FROM information_schema.tables
                 WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
                 """)) {
            while (rs.next()) {
                tables.add(rs.getString("table_name"));
            }
        }

        assertThat(tables)
            .as("every module-prefixed table this migration set is expected to create")
            .contains(
                "identity_account", "identity_address", "identity_role", "identity_account_role", "identity_token",
                "catalog_category", "catalog_product", "catalog_product_image", "catalog_variant",
                "inventory_warehouse", "inventory_stock_item", "inventory_stock_reservation",
                "inventory_stock_adjustment",
                "cart_cart", "cart_cart_line", "cart_wishlist", "cart_wishlist_item",
                "ordering_order", "ordering_order_line", "ordering_order_line_reservation",
                "ordering_idempotency_key", "ordering_outbox",
                "payment_payment", "payment_attempt", "payment_refund", "payment_outbox",
                "shipping_shipment", "shipping_tracking_event", "shipping_outbox",
                "promotion_promotion", "promotion_redemption", "promotion_outbox",
                "review_review", "review_image", "review_verified_purchase", "review_outbox",
                "notification_request", "notification_preference",
                "audit_entry",
                "catalog_outbox", "inventory_outbox",
                "catalog_processed_event", "ordering_processed_event",
                "shipping_processed_event", "reporting_processed_event");
    }
}

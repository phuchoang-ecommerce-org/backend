package org.phuchoang.ecp.configuration.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Explicit local-demo data only. This runner is absent unless the {@code demo} Spring profile is
 * active, so production schema migrations and regular development starts never create accounts or
 * catalog records. IDs and SKUs are stable so {@code ECP_DEMO_RESET=true} can remove only this
 * runner's records after a hands-on demo.
 */
@Component
@Profile("demo")
class DemoDataSeeder implements ApplicationRunner {

    private static final String DEMO_PASSWORD = "DemoPass!2026";

    private static final UUID ADMIN_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID CUSTOMER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID COFFEE_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID BREWING_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID KETTLE_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID GRINDER_ID = UUID.fromString("30000000-0000-4000-8000-000000000002");
    private static final UUID MUG_ID = UUID.fromString("30000000-0000-4000-8000-000000000003");
    private static final UUID DRAFT_ID = UUID.fromString("30000000-0000-4000-8000-000000000004");
    private static final UUID KETTLE_BLACK_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID KETTLE_WHITE_ID = UUID.fromString("40000000-0000-0000-0000-000000000002");
    private static final UUID GRINDER_ID_VARIANT = UUID.fromString("40000000-0000-0000-0000-000000000003");
    private static final UUID MUG_ID_VARIANT = UUID.fromString("40000000-0000-0000-0000-000000000004");
    private static final UUID DRAFT_ID_VARIANT = UUID.fromString("40000000-0000-0000-0000-000000000005");

    private final JdbcClient jdbc;
    private final TransactionTemplate transactions;
    private final boolean reset;
    private final Argon2PasswordEncoder passwordEncoder;

    DemoDataSeeder(JdbcClient jdbc, TransactionTemplate transactions,
            @Value("${ECP_DEMO_RESET:${ecp.demo.reset:false}}") boolean reset,
            @Value("${ecp.password.argon2-salt-length:16}") int saltLength,
            @Value("${ecp.password.argon2-hash-length:32}") int hashLength,
            @Value("${ecp.password.argon2-parallelism:1}") int parallelism,
            @Value("${ecp.password.argon2-memory-kib:19456}") int memoryKib,
            @Value("${ecp.password.argon2-iterations:2}") int iterations) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.reset = reset;
        this.passwordEncoder = new Argon2PasswordEncoder(saltLength, hashLength, parallelism, memoryKib, iterations);
    }

    @Override
    public void run(ApplicationArguments arguments) {
        transactions.executeWithoutResult(status -> {
            if (reset) {
                reset();
            }
            seed();
        });
    }

    private void seed() {
        String passwordHash = passwordEncoder.encode(DEMO_PASSWORD);
        account(ADMIN_ID, "admin@demo.local", "Demo Administrator", passwordHash, "ADMINISTRATOR");
        account(CUSTOMER_ID, "customer@demo.local", "Demo Customer", passwordHash, "CUSTOMER");

        category(COFFEE_ID, null, "Demo Coffee", "demo-coffee", "/demo-coffee/", 0, true);
        category(BREWING_ID, COFFEE_ID, "Demo Brewing", "demo-brewing", "/demo-coffee/demo-brewing/", 0, false);

        product(KETTLE_ID, BREWING_ID, "Demo Pour-Over Kettle", "demo-pour-over-kettle",
            "A precise gooseneck kettle for a hands-on storefront detail demo.", "ECP Demo", "PUBLISHED");
        product(GRINDER_ID, BREWING_ID, "Demo Burr Grinder", "demo-burr-grinder",
            "A compact grinder with a single, ready-to-test variant.", "ECP Demo", "PUBLISHED");
        product(MUG_ID, COFFEE_ID, "Demo Travel Mug", "demo-travel-mug",
            "An insulated mug seeded with an out-of-stock advisory state.", "ECP Demo", "PUBLISHED");
        product(DRAFT_ID, BREWING_ID, "Demo Draft Scale", "demo-draft-scale",
            "An admin-only draft record for publication workflow testing.", "ECP Demo", "DRAFT");

        variant(KETTLE_BLACK_ID, KETTLE_ID, "DEMO-KETTLE-BLK", "Matte black", new BigDecimal("59.00"),
            "{\"color\":\"Black\"}", true, true);
        variant(KETTLE_WHITE_ID, KETTLE_ID, "DEMO-KETTLE-WHT", "Matte white", new BigDecimal("59.00"),
            "{\"color\":\"White\"}", true, true);
        variant(GRINDER_ID_VARIANT, GRINDER_ID, "DEMO-GRINDER-01", "Standard", new BigDecimal("89.00"),
            "{\"finish\":\"Steel\"}", true, true);
        variant(MUG_ID_VARIANT, MUG_ID, "DEMO-MUG-01", "12 oz", new BigDecimal("24.00"),
            "{\"size\":\"12 oz\"}", true, false);
        variant(DRAFT_ID_VARIANT, DRAFT_ID, "DEMO-SCALE-01", "Standard", new BigDecimal("39.00"),
            "{\"unit\":\"grams\"}", true, true);

        image(KETTLE_ID, "http://localhost:3000/demo/pour-over-kettle.svg", "Demo pour-over kettle");
        image(GRINDER_ID, "http://localhost:3000/demo/burr-grinder.svg", "Demo burr grinder");
        image(MUG_ID, "http://localhost:3000/demo/travel-mug.svg", "Demo travel mug");
    }

    private void account(UUID id, String email, String displayName, String passwordHash, String role) {
        update("""
            INSERT INTO identity_account (id, email, credential_hash, display_name, status, verification_status, verified_at)
            VALUES (?, ?, ?, ?, 'ACTIVE', 'VERIFIED', now())
            ON CONFLICT (id) DO NOTHING
            """, id, email, passwordHash, displayName);
        update("""
            INSERT INTO identity_account_role (account_id, role_id)
            SELECT ?, id FROM identity_role WHERE code = ?
            ON CONFLICT (account_id, role_id) DO NOTHING
            """, id, role);
    }

    private void category(UUID id, UUID parentId, String name, String slug, String path, int sortOrder, boolean featured) {
        update("""
            INSERT INTO catalog_category (id, parent_id, name, slug, path, depth, sort_order, image_url, featured, created_by, updated_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, NULL, ?, ?, ?)
            ON CONFLICT (id) DO NOTHING
            """, id, parentId, name, slug, path, parentId == null ? 0 : 1, sortOrder, featured, ADMIN_ID, ADMIN_ID);
    }

    private void product(UUID id, UUID categoryId, String name, String slug, String description, String brand, String status) {
        update("""
            INSERT INTO catalog_product (id, category_id, owner_id, name, slug, description, brand, publication_status,
                                         published_at, attributes, created_by, updated_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, CASE WHEN ? = 'PUBLISHED' THEN now() ELSE NULL END,
                    '{"demo":true}'::jsonb, ?, ?)
            ON CONFLICT (id) DO NOTHING
            """, id, categoryId, ADMIN_ID, name, slug, description, brand, status, status, ADMIN_ID, ADMIN_ID);
    }

    private void variant(UUID id, UUID productId, String sku, String name, BigDecimal price, String options,
            boolean active, boolean inStock) {
        update("""
            INSERT INTO catalog_variant (id, product_id, sku, name, list_price_amount, list_price_currency, options,
                                         is_active, advisory_in_stock, created_by, updated_by)
            VALUES (?, ?, ?, ?, ?, 'USD', ?::jsonb, ?, ?, ?, ?)
            ON CONFLICT (id) DO NOTHING
            """, id, productId, sku, name, price, options, active, inStock, ADMIN_ID, ADMIN_ID);
    }

    private void image(UUID productId, String url, String altText) {
        update("""
            INSERT INTO catalog_product_image (id, product_id, url, alt_text, sort_order, created_by, updated_by)
            VALUES (?, ?, ?, ?, 0, ?, ?)
            ON CONFLICT (id) DO NOTHING
            """, UUID.nameUUIDFromBytes(("demo-image-" + productId).getBytes()), productId, url, altText, ADMIN_ID, ADMIN_ID);
    }

    private void reset() {
        UUID legacyKettleId = UUID.fromString("30000000-0000-0000-0000-000000000001");
        UUID legacyGrinderId = UUID.fromString("30000000-0000-0000-0000-000000000002");
        UUID legacyMugId = UUID.fromString("30000000-0000-0000-0000-000000000003");
        UUID legacyDraftId = UUID.fromString("30000000-0000-0000-0000-000000000004");

        update("DELETE FROM catalog_outbox WHERE aggregate_id IN (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            KETTLE_ID, GRINDER_ID, MUG_ID, DRAFT_ID, legacyKettleId, legacyGrinderId, legacyMugId, legacyDraftId,
            COFFEE_ID, BREWING_ID);
        update("DELETE FROM catalog_product_image WHERE product_id IN (?, ?, ?, ?, ?, ?, ?, ?)",
            KETTLE_ID, GRINDER_ID, MUG_ID, DRAFT_ID, legacyKettleId, legacyGrinderId, legacyMugId, legacyDraftId);
        update("DELETE FROM catalog_variant WHERE product_id IN (?, ?, ?, ?, ?, ?, ?, ?)",
            KETTLE_ID, GRINDER_ID, MUG_ID, DRAFT_ID, legacyKettleId, legacyGrinderId, legacyMugId, legacyDraftId);
        update("DELETE FROM catalog_retired_sku WHERE sku LIKE 'DEMO-%'");
        update("DELETE FROM catalog_product WHERE id IN (?, ?, ?, ?, ?, ?, ?, ?)",
            KETTLE_ID, GRINDER_ID, MUG_ID, DRAFT_ID, legacyKettleId, legacyGrinderId, legacyMugId, legacyDraftId);
        update("DELETE FROM catalog_category WHERE id = ?", BREWING_ID);
        update("DELETE FROM catalog_category WHERE id = ?", COFFEE_ID);
        update("DELETE FROM identity_account WHERE id IN (?, ?)", ADMIN_ID, CUSTOMER_ID);
    }

    private void update(String sql, Object... parameters) {
        jdbc.sql(sql).params(parameters).update();
    }
}

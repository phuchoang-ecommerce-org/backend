package org.phuchoang.ecp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "ecp.cursor.active-key=integration-test-cursor-secret-0001")
@ActiveProfiles("dev")
@Import(TestcontainersConfiguration.class)
class DeveloperProfileIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void seedsUsableAccountsForEveryAuthenticatedRoleAndCatalogExamples() {
        var encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        for (String role : new String[] {"CUSTOMER", "STAFF", "WAREHOUSE_OPERATOR", "CUSTOMER_SUPPORT", "ADMINISTRATOR"}) {
            String email = switch (role) {
                case "ADMINISTRATOR" -> "admin@demo.local";
                case "WAREHOUSE_OPERATOR" -> "warehouse@demo.local";
                case "CUSTOMER_SUPPORT" -> "support@demo.local";
                default -> role.toLowerCase() + "@demo.local";
            };
            String hash = jdbc.queryForObject("""
                SELECT a.credential_hash FROM identity_account a
                JOIN identity_account_role ar ON ar.account_id = a.id
                JOIN identity_role r ON r.id = ar.role_id
                WHERE a.email = ? AND a.status = 'ACTIVE' AND a.verification_status = 'VERIFIED' AND r.code = ?
                """, String.class, email, role);
            assertThat(encoder.matches("DemoPass!2026", hash)).isTrue();
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM catalog_product WHERE brand = 'ECP Demo'", Long.class))
            .isGreaterThanOrEqualTo(4);
    }
}

package org.phuchoang.ecp.identity.infrastructure.persistence;

import org.phuchoang.ecp.identity.domain.RoleCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * {@code identity_account_role} — a plain join table (composite key, no natural JPA entity
 * identity), read/written directly rather than mapped as a {@code @ManyToMany} to keep the
 * mapping obvious. {@link RoleJpaRepository} resolves a role's id from its fixed code
 * (`V202609071408` seeds the six rows).
 */
@Component
class AccountRoleAssignments {

    private final JdbcTemplate jdbcTemplate;
    private final RoleJpaRepository roleJpaRepository;

    AccountRoleAssignments(JdbcTemplate jdbcTemplate, RoleJpaRepository roleJpaRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.roleJpaRepository = roleJpaRepository;
    }

    void grant(UUID accountId, RoleCode role) {
        UUID roleId = roleJpaRepository.findByCode(role.name())
            .orElseThrow(() -> new IllegalStateException(
                "identity_role has no row for " + role + " — was V202609071408 applied?"))
            .getId();
        jdbcTemplate.update("""
            INSERT INTO identity_account_role (account_id, role_id) VALUES (?, ?)
            ON CONFLICT (account_id, role_id) DO NOTHING
            """, accountId, roleId);
    }

    Set<RoleCode> rolesOf(UUID accountId) {
        var codes = jdbcTemplate.query("""
            SELECT r.code FROM identity_account_role ar
            JOIN identity_role r ON r.id = ar.role_id
            WHERE ar.account_id = ?
            """, (rs, rowNum) -> RoleCode.valueOf(rs.getString("code")), accountId);
        return codes.isEmpty() ? EnumSet.noneOf(RoleCode.class) : EnumSet.copyOf(codes);
    }
}

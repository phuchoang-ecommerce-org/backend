package org.phuchoang.ecp.identity.internal.infrastructure.persistence.account;

import org.phuchoang.ecp.identity.internal.domain.model.RoleCode;
import org.springframework.jdbc.core.simple.JdbcClient;
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

    private final JdbcClient jdbc;
    private final RoleJpaRepository roleJpaRepository;

    AccountRoleAssignments(JdbcClient jdbc, RoleJpaRepository roleJpaRepository) {
        this.jdbc = jdbc;
        this.roleJpaRepository = roleJpaRepository;
    }

    void grant(UUID accountId, RoleCode role) {
        UUID roleId = roleJpaRepository.findByCode(role.name())
            .orElseThrow(() -> new IllegalStateException(
                "identity_role has no row for " + role + " — was V202609071408 applied?"))
            .getId();
        jdbc.sql("""
            INSERT INTO identity_account_role (account_id, role_id) VALUES (?, ?)
            ON CONFLICT (account_id, role_id) DO NOTHING
            """).params(accountId, roleId).update();
    }

    Set<RoleCode> rolesOf(UUID accountId) {
        var codes = jdbc.sql("""
            SELECT r.code FROM identity_account_role ar
            JOIN identity_role r ON r.id = ar.role_id
            WHERE ar.account_id = ?
            """).param(accountId).query((rs, rowNum) -> RoleCode.valueOf(rs.getString("code"))).list();
        return codes.isEmpty() ? EnumSet.noneOf(RoleCode.class) : EnumSet.copyOf(codes);
    }
}

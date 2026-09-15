package org.phuchoang.ecp.identity.infrastructure.persistence.account;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** JPA mapping of {@code identity_role} — fixed, seeded reference data (`V202609071408`). */
@Entity
@Table(name = "identity_role")
class RoleEntity {

    @Id
    private UUID id;

    private String code;

    protected RoleEntity() {
        // JPA
    }

    UUID getId() {
        return id;
    }

    String getCode() {
        return code;
    }
}

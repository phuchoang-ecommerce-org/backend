package org.phuchoang.ecp.identity.internal.infrastructure.persistence.account;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** JPA mapping of {@code identity_role} — fixed, seeded reference data (`V202609071408`). */
@Entity
@Table(name = "identity_role")
@Getter(AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class RoleEntity {

    @Id
    private UUID id;

    private String code;

}

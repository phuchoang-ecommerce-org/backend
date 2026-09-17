package org.phuchoang.ecp.identity.internal.infrastructure.persistence.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Persistence lookup for fixed role reference data used during account-role reconstruction. */
interface RoleJpaRepository extends JpaRepository<RoleEntity, UUID> {
    Optional<RoleEntity> findByCode(String code);
}

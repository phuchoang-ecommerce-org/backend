package org.phuchoang.ecp.identity.internal.infrastructure.persistence.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Persistence lookup for the Account aggregate; email lookup supports credential and uniqueness workflows. */
interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID> {
    Optional<AccountEntity> findByEmailIgnoreCase(String email);
}

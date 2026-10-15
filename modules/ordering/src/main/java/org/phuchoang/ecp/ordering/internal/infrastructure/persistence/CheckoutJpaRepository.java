package org.phuchoang.ecp.ordering.internal.infrastructure.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface CheckoutJpaRepository extends JpaRepository<CheckoutEntity, UUID> {
    @EntityGraph(attributePaths = "lines") Optional<CheckoutEntity> findById(UUID id);
    @EntityGraph(attributePaths = "lines") Optional<CheckoutEntity> findByCustomerIdAndStatus(UUID customerId, String status);
}

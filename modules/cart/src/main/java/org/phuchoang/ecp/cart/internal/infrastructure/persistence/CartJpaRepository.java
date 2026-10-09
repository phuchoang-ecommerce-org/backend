package org.phuchoang.ecp.cart.internal.infrastructure.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface CartJpaRepository extends JpaRepository<CartEntity, UUID> {
    @EntityGraph(attributePaths = "lines") Optional<CartEntity> findAggregateById(UUID id);
    @EntityGraph(attributePaths = "lines") Optional<CartEntity> findByCustomerIdAndStatus(UUID customerId, String status);
    @EntityGraph(attributePaths = "lines") Optional<CartEntity> findByGuestTokenAndStatus(String guestToken, String status);
}

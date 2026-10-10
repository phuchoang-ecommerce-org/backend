package org.phuchoang.ecp.cart.internal.infrastructure.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface CartJpaRepository extends JpaRepository<CartEntity, UUID> {
    @EntityGraph(attributePaths = "lines") Optional<CartEntity> findAggregateById(UUID id);
    @EntityGraph(attributePaths = "lines") Optional<CartEntity> findByCustomerIdAndStatus(UUID customerId, String status);
    @EntityGraph(attributePaths = "lines") Optional<CartEntity> findByGuestTokenAndStatus(String guestToken, String status);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @EntityGraph(attributePaths = "lines") Optional<CartEntity> findLockedByCustomerIdAndStatus(UUID customerId, String status);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @EntityGraph(attributePaths = "lines") Optional<CartEntity> findLockedByGuestTokenAndStatus(String guestToken, String status);
    @EntityGraph(attributePaths = "lines") List<CartEntity> findTop100ByStatusAndExpiresAtLessThanEqualOrderByExpiresAt(String status, Instant now);
}

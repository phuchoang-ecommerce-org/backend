package org.phuchoang.ecp.cart.internal.infrastructure.persistence;

import org.phuchoang.ecp.cart.internal.domain.model.Cart;
import org.phuchoang.ecp.cart.internal.domain.model.CartLine;
import org.phuchoang.ecp.cart.internal.domain.model.CartStatus;
import org.phuchoang.ecp.cart.internal.domain.repository.CartRepository;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Aggregate persistence adapter; JPA's version column protects concurrent cart edits. */
@Repository
class JpaCartRepositoryAdapter implements CartRepository {
    private final CartJpaRepository carts;
    private final Clock clock;
    JpaCartRepositoryAdapter(CartJpaRepository carts, Clock clock) { this.carts = carts; this.clock = clock; }
    @Override public Optional<Cart> findById(UUID id) { return carts.findAggregateById(id).map(JpaCartRepositoryAdapter::toDomain); }
    @Override public Optional<Cart> findActiveByCustomerId(UUID customerId) { return carts.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE.name()).map(JpaCartRepositoryAdapter::toDomain); }
    @Override public Optional<Cart> findActiveByGuestToken(String token) { return carts.findByGuestTokenAndStatus(token, CartStatus.ACTIVE.name()).map(JpaCartRepositoryAdapter::toDomain); }
    @Override public Optional<Cart> findActiveByGuestTokenForUpdate(String token) { return carts.findLockedByGuestTokenAndStatus(token, CartStatus.ACTIVE.name()).map(JpaCartRepositoryAdapter::toDomain); }
    @Override public Optional<Cart> findActiveByCustomerIdForUpdate(UUID customerId) { return carts.findLockedByCustomerIdAndStatus(customerId, CartStatus.ACTIVE.name()).map(JpaCartRepositoryAdapter::toDomain); }
    @Override public List<Cart> findExpiredActiveAt(Instant now, int limit) {
        return carts.findTop100ByStatusAndExpiresAtLessThanEqualOrderByExpiresAt(CartStatus.ACTIVE.name(), now).stream()
            .limit(limit).map(JpaCartRepositoryAdapter::toDomain).toList();
    }
    @Override public Cart save(Cart cart) {
        CartEntity entity = carts.findAggregateById(cart.id()).orElseGet(() -> new CartEntity(cart.id(), cart.customerId(), cart.guestToken(),
            cart.status().name(), cart.lastActivityAt(), cart.expiresAt(), cart.mergedIntoId(), clock.instant()));
        Instant now = clock.instant();
        List<CartLineEntity> lines = cart.lines().stream().map(line -> new CartLineEntity(line.id(), line.variantId(), line.sku(), line.quantity(), line.addedAt(), now)).toList();
        entity.synchronize(cart.status().name(), cart.lastActivityAt(), cart.expiresAt(), cart.mergedIntoId(), lines, now);
        return toDomain(carts.saveAndFlush(entity));
    }
    private static Cart toDomain(CartEntity entity) {
        return new Cart(entity.id(), entity.customerId(), entity.guestToken(), CartStatus.valueOf(entity.status()), entity.lastActivityAt(),
            entity.expiresAt(), entity.mergedIntoId(), entity.version(), entity.lines().stream().map(line -> new CartLine(line.id(), line.variantId(),
                line.sku(), line.quantity(), line.addedAt())).toList());
    }
}

package org.phuchoang.ecp.cart.internal.infrastructure.persistence;

import org.phuchoang.ecp.cart.internal.domain.model.Cart;
import org.phuchoang.ecp.cart.internal.domain.model.CartStatus;
import org.phuchoang.ecp.cart.internal.domain.repository.CartRepository;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Aggregate persistence adapter; JPA's version column protects concurrent cart
 * edits.
 */
@Repository
class JpaCartRepositoryAdapter implements CartRepository {
  private final CartJpaRepository carts;
  private final CartJpaMapper mapper;
  private final Clock clock;

  JpaCartRepositoryAdapter(CartJpaRepository carts, CartJpaMapper mapper, Clock clock) {
    this.carts = carts;
    this.mapper = mapper;
    this.clock = clock;
  }

  @Override
  public Optional<Cart> findById(UUID id) {
    return carts.findAggregateById(id).map(mapper::toDomain);
  }

  @Override
  public Optional<Cart> findActiveByCustomerId(UUID customerId) {
    return carts.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE.name()).map(mapper::toDomain);
  }

  @Override
  public Optional<Cart> findActiveByGuestToken(String token) {
    return carts.findByGuestTokenAndStatus(token, CartStatus.ACTIVE.name()).map(mapper::toDomain);
  }

  @Override
  public Optional<Cart> findActiveByGuestTokenForUpdate(String token) {
    return carts.findLockedByGuestTokenAndStatus(token, CartStatus.ACTIVE.name()).map(mapper::toDomain);
  }

  @Override
  public Optional<Cart> findActiveByCustomerIdForUpdate(UUID customerId) {
    return carts.findLockedByCustomerIdAndStatus(customerId, CartStatus.ACTIVE.name()).map(mapper::toDomain);
  }

  @Override
  public List<Cart> findExpiredActiveAt(Instant now, int limit) {
    return carts.findTop100ByStatusAndExpiresAtLessThanEqualOrderByExpiresAt(CartStatus.ACTIVE.name(), now).stream()
        .limit(limit).map(mapper::toDomain).toList();
  }

  @Override
  public Cart save(Cart cart) {
    Instant now = clock.instant();
    CartEntity entity = carts.findAggregateById(cart.id()).orElse(null);
    if (entity == null) {
      entity = mapper.toEntity(cart, now);
    } else {
      mapper.updateEntity(cart, entity);
      entity.synchronizeLines(mapper.toEntities(cart.lines(), now), now);
    }
    return mapper.toDomain(carts.saveAndFlush(entity));
  }
}

package org.phuchoang.ecp.cart.internal.domain.repository;

import org.jmolecules.ddd.annotation.Repository;
import org.phuchoang.ecp.cart.internal.domain.model.Cart;

import java.util.Optional;
import java.util.UUID;

/**
 * Aggregate-oriented persistence port; query projections are intentionally
 * separate.
 */
@Repository
public interface CartRepository {
  Optional<Cart> findById(UUID id);

  Optional<Cart> findActiveByCustomerId(UUID customerId);

  Optional<Cart> findActiveByGuestToken(String guestToken);

  Cart save(Cart cart);
}

package org.phuchoang.ecp.cart.internal.domain.repository;

import org.jmolecules.ddd.annotation.Repository;
import org.phuchoang.ecp.cart.internal.domain.model.Cart;

import java.time.Instant;
import java.util.List;
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

  /** Locks the guest aggregate so concurrent claims cannot lose its lines. */
  Optional<Cart> findActiveByGuestTokenForUpdate(String guestToken);

  /** Locks an existing customer aggregate while a guest cart is folded into it. */
  Optional<Cart> findActiveByCustomerIdForUpdate(UUID customerId);

  List<Cart> findExpiredActiveAt(Instant now, int limit);

  Cart save(Cart cart);
}

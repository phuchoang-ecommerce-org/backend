package org.phuchoang.ecp.cart.internal.domain.service;

import org.jmolecules.ddd.annotation.Service;
import org.phuchoang.ecp.cart.internal.domain.model.Cart;
import org.phuchoang.ecp.cart.internal.domain.model.CartLine;
import org.phuchoang.ecp.cart.internal.domain.model.CartOwner;
import org.phuchoang.ecp.cart.internal.domain.repository.CartRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Authoritative command-side cart workflow. All aggregate persistence caused by
 * a cart state
 * transition is performed here, after this service has applied ownership,
 * expiry, and stock rules.
 */
@Service
public final class CartCommandService {
  private static final long GUEST_LIFETIME_DAYS = 7;
  private static final long CUSTOMER_LIFETIME_DAYS = 30;

  private final CartRepository carts;

  public CartCommandService(CartRepository carts) {
    this.carts = carts;
  }

  public Cart current(CartOwner owner, Instant now) {
    return findCurrent(owner).orElseGet(() -> carts.save(newCart(owner, now)));
  }

  public Cart add(CartOwner owner, UUID cartId, Variant variant, int quantity, Instant now) {
    Cart cart = resolve(owner, cartId, true, now);
    int combined = cart.lines().stream().filter(line -> line.variantId().equals(variant.id()))
        .mapToInt(CartLine::quantity).findFirst().orElse(0) + quantity;
    requireAvailable(variant, combined);
    return carts.save(cart.add(variant.id(), variant.sku(), quantity, now));
  }

  public Cart changeQuantity(CartOwner owner, UUID cartId, UUID lineId, int quantity, VariantLookup variants,
      Instant now) {
    Cart cart = resolve(owner, cartId, false, now);
    CartLine line = cart.lines().stream().filter(candidate -> candidate.id().equals(lineId)).findFirst().orElse(null);
    if (line == null)
      return cart; // E2: a deleted line is not resurrected by a stale update.
    if (quantity > 0)
      requireAvailable(java.util.Objects.requireNonNull(variants.find(line.variantId()), "variant"), quantity);
    return carts.save(cart.changeQuantity(lineId, quantity, now));
  }

  public void remove(CartOwner owner, UUID cartId, UUID lineId, Instant now) {
    Cart cart = resolve(owner, cartId, false, now);
    Cart changed = cart.remove(lineId, now);
    if (changed != cart)
      carts.save(changed); // an absent line is a successful no-op.
  }

  private Cart resolve(CartOwner owner, UUID cartId, boolean replaceExpired, Instant now) {
    Cart cart = cartId == null ? findCurrent(owner).orElseGet(() -> newCart(owner, now))
        : carts.findById(cartId).filter(owner::owns).orElseThrow(CartNotFound::new);
    if (cart.isExpiredAt(now)) {
      if (!replaceExpired)
        throw new CartNotFound();
      return newCart(owner, now);
    }
    return cart;
  }

  private Optional<Cart> findCurrent(CartOwner owner) {
    return owner.customerId() != null ? carts.findActiveByCustomerId(owner.customerId())
        : carts.findActiveByGuestToken(owner.guestToken());
  }

  private static Cart newCart(CartOwner owner, Instant now) {
    return owner.customerId() != null
        ? Cart.customer(UUID.randomUUID(), owner.customerId(), now, now.plus(CUSTOMER_LIFETIME_DAYS, ChronoUnit.DAYS))
        : Cart.guest(UUID.randomUUID(), owner.guestToken(), now, now.plus(GUEST_LIFETIME_DAYS, ChronoUnit.DAYS));
  }

  private static void requireAvailable(Variant variant, int quantity) {
    if (variant.availableQuantity() != null && quantity > variant.availableQuantity()) {
      throw new QuantityExceeded(variant.availableQuantity());
    }
  }

  public record Variant(UUID id, String sku, Integer availableQuantity) {
  }

  @FunctionalInterface
  public interface VariantLookup {
    Variant find(UUID variantId);
  }

  public static final class CartNotFound extends RuntimeException {
  }

  public static final class QuantityExceeded extends RuntimeException {
    private final int availableQuantity;

    public QuantityExceeded(int availableQuantity) {
      this.availableQuantity = availableQuantity;
    }

    public int availableQuantity() {
      return availableQuantity;
    }
  }
}

package org.phuchoang.ecp.cart.internal.domain.service;

import org.phuchoang.ecp.cart.internal.domain.model.Cart;
import org.phuchoang.ecp.cart.internal.domain.model.CartLine;
import org.phuchoang.ecp.cart.internal.domain.model.CartOwner;
import org.phuchoang.ecp.cart.internal.domain.model.CartMergeNotice;
import org.phuchoang.ecp.cart.internal.domain.policy.CartMergePolicy;
import org.phuchoang.ecp.cart.internal.domain.repository.CartRepository;

import java.time.Instant;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Authoritative command-side cart workflow. All aggregate persistence caused by
 * a cart state
 * transition is performed here, after this service has applied ownership,
 * expiry, and stock rules.
 */
public final class CartCommandService {
  private final CartRepository carts;
  private final Duration guestLifetime;
  private final Duration customerLifetime;
  private final CartMergePolicy mergePolicy = new CartMergePolicy();

  public CartCommandService(CartRepository carts, Duration guestLifetime, Duration customerLifetime) {
    this.carts = carts;
    this.guestLifetime = guestLifetime;
    this.customerLifetime = customerLifetime;
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

  /**
   * Locks and changes both authoritative aggregates as one command. Catalog
   * availability is supplied as an input because it belongs to Catalog, not Cart.
   */
  public MergeResult mergeGuestCart(UUID customerId, String guestToken, MergeVariantLookup variants, Instant now) {
    Cart guest = carts.findActiveByGuestTokenForUpdate(guestToken).orElse(null);
    if (guest == null || guest.lines().isEmpty()) return MergeResult.notFound();

    Cart customer = carts.findActiveByCustomerIdForUpdate(customerId)
        .orElseGet(() -> newCustomerCart(customerId, now));
    Map<UUID, CartMergePolicy.VariantAvailability> availability = new HashMap<>();
    for (CartLine line : guest.lines()) {
      availability.computeIfAbsent(line.variantId(), variants::find);
    }
    CartMergePolicy.MergePlan plan = mergePolicy.merge(customer.lines(), guest.lines(), availability);
    Cart persistedCustomer = carts.save(customer.replaceLines(plan.lines(), now));
    carts.save(guest.mergeInto(persistedCustomer.id()));
    return new MergeResult(true, plan.notices());
  }

  /** Marks due carts inactive unless an application-supplied checkout deferral applies. */
  public void expireDueCarts(Instant now, int limit, Predicate<UUID> hasActiveCheckoutOrDraftOrder) {
    carts.findExpiredActiveAt(now, limit).forEach(cart -> {
      if (!hasActiveCheckoutOrDraftOrder.test(cart.id())) {
        carts.save(cart.expire());
      }
    });
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

  private Cart newCustomerCart(UUID customerId, Instant now) {
    return Cart.customer(UUID.randomUUID(), customerId, now, now.plus(customerLifetime));
  }

  private Cart newCart(CartOwner owner, Instant now) {
    return owner.customerId() != null
        ? newCustomerCart(owner.customerId(), now)
        : Cart.guest(UUID.randomUUID(), owner.guestToken(), now, now.plus(guestLifetime));
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

  @FunctionalInterface
  public interface MergeVariantLookup {
    CartMergePolicy.VariantAvailability find(UUID variantId);
  }

  public record MergeResult(boolean merged, List<CartMergeNotice> notices) {
    public MergeResult {
      notices = List.copyOf(notices);
    }

    public static MergeResult notFound() {
      return new MergeResult(false, List.of());
    }
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

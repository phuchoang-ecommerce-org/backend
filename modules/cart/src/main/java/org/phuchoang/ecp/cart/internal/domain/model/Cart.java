package org.phuchoang.ecp.cart.internal.domain.model;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Consistency boundary for a shopper's unpriced selection. Catalog price and availability are
 * deliberately evaluated outside this aggregate at command/query time.
 */
@AggregateRoot
public record Cart(@Identity UUID id, UUID customerId, String guestToken, CartStatus status, Instant lastActivityAt,
                   Instant expiresAt, UUID mergedIntoId, long version, List<CartLine> lines) {
    public Cart {
        Objects.requireNonNull(id, "id");
        if (customerId == null && (guestToken == null || guestToken.isBlank())) {
            throw new IllegalArgumentException("A cart requires a customer or guest owner.");
        }
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(lastActivityAt, "lastActivityAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
        lines = List.copyOf(lines);
    }

    public static Cart guest(UUID id, String guestToken, Instant now, Instant expiresAt) {
        return new Cart(id, null, guestToken, CartStatus.ACTIVE, now, expiresAt, null, 0, List.of());
    }

    public static Cart customer(UUID id, UUID customerId, Instant now, Instant expiresAt) {
        return new Cart(id, customerId, null, CartStatus.ACTIVE, now, expiresAt, null, 0, List.of());
    }

    public Cart add(UUID variantId, String sku, int quantity, Instant now) {
        requireActive();
        if (quantity <= 0) throw new IllegalArgumentException("Cart line quantity must be positive.");
        List<CartLine> updated = new ArrayList<>(lines);
        for (int index = 0; index < updated.size(); index++) {
            CartLine line = updated.get(index);
            if (line.variantId().equals(variantId)) {
                updated.set(index, line.withQuantity(Math.addExact(line.quantity(), quantity)));
                return withLines(updated, now);
            }
        }
        updated.add(new CartLine(UUID.randomUUID(), variantId, sku, quantity, now));
        return withLines(updated, now);
    }

    public Cart changeQuantity(UUID lineId, int quantity, Instant now) {
        requireActive();
        List<CartLine> updated = new ArrayList<>(lines);
        for (int index = 0; index < updated.size(); index++) {
            if (updated.get(index).id().equals(lineId)) {
                if (quantity == 0) updated.remove(index);
                else updated.set(index, updated.get(index).withQuantity(quantity));
                return withLines(updated, now);
            }
        }
        return this;
    }

    public Cart remove(UUID lineId, Instant now) {
        return changeQuantity(lineId, 0, now);
    }

    public Cart expire() {
        return new Cart(id, customerId, guestToken, CartStatus.EXPIRED, lastActivityAt, expiresAt, mergedIntoId, version, lines);
    }

    public boolean isExpiredAt(Instant now) {
        return status == CartStatus.EXPIRED || !expiresAt.isAfter(now);
    }

    private Cart withLines(List<CartLine> updated, Instant now) {
        return new Cart(id, customerId, guestToken, status, now, expiresAt, mergedIntoId, version, updated);
    }

    private void requireActive() {
        if (status != CartStatus.ACTIVE) throw new IllegalStateException("Only an active cart can be changed.");
    }
}

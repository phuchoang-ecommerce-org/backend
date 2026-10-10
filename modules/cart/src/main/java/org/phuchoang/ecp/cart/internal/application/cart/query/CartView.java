package org.phuchoang.ecp.cart.internal.application.cart.query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Strongly consistent application read model for a cart. */
public record CartView(UUID id, UUID customerId, String status, List<CartLineView> lines, MoneyView subtotal,
                       Instant lastActivityAt, Instant expiresAt, boolean expired) {
    public CartView {
        lines = List.copyOf(lines);
    }
}

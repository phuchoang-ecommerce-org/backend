package org.phuchoang.ecp.cart.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Strongly consistent Cart view, priced from Catalog at read time. */
public record CartView(UUID id, UUID customerId, String status, List<CartLineView> lines, MoneyView subtotal,
                       Instant lastActivityAt, Instant expiresAt, boolean expired) {
    public CartView { lines = List.copyOf(lines); }
}

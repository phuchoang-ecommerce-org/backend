package org.phuchoang.ecp.inventory.api.reservation;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** The selected allocations that Ordering asks Inventory to hold as one indivisible operation. */
public record ReserveStockCommand(UUID orderId, List<Line> lines, Instant expiresAt) {

    public ReserveStockCommand {
        Objects.requireNonNull(orderId, "orderId");
        lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("A stock reservation needs at least one line.");
        }
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    /** A warehouse allocation selected by Ordering; Inventory does not invent allocation policy. */
    public record Line(UUID orderLineId, UUID stockItemId, String sku, int quantity, boolean published) {
        public Line {
            Objects.requireNonNull(orderLineId, "orderLineId");
            Objects.requireNonNull(stockItemId, "stockItemId");
            if (sku == null || sku.isBlank()) {
                throw new IllegalArgumentException("sku must not be blank.");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("quantity must be positive.");
            }
        }
    }
}

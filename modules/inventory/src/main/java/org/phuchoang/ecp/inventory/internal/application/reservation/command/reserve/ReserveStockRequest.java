package org.phuchoang.ecp.inventory.internal.application.reservation.command.reserve;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Application input for the all-or-nothing stock reservation use case. */
public record ReserveStockRequest(UUID orderId, List<Line> lines, Instant expiresAt) {

    public ReserveStockRequest {
        Objects.requireNonNull(orderId, "orderId");
        lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("A stock reservation needs at least one line.");
        }
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

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

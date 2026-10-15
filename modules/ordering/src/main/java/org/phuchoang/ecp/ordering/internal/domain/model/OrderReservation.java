package org.phuchoang.ecp.ordering.internal.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Immutable record of stock held for a placed order line; the referenced aggregate remains Inventory-owned. */
public record OrderReservation(UUID orderLineId, UUID stockItemId, UUID warehouseId, UUID reservationId, int quantity) {
    public OrderReservation {
        Objects.requireNonNull(orderLineId, "orderLineId");
        Objects.requireNonNull(stockItemId, "stockItemId");
        Objects.requireNonNull(warehouseId, "warehouseId");
        Objects.requireNonNull(reservationId, "reservationId");
        if (quantity <= 0) throw new IllegalArgumentException("Reservation quantity must be positive.");
    }
}

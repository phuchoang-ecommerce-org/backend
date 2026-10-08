package org.phuchoang.ecp.inventory.api.reservation;

import java.util.Objects;
import java.util.UUID;

/** Identifies the child reservation together with its owning StockItem aggregate. */
public record ReservationReference(UUID stockItemId, UUID reservationId) {
    public ReservationReference {
        Objects.requireNonNull(stockItemId, "stockItemId");
        Objects.requireNonNull(reservationId, "reservationId");
    }
}

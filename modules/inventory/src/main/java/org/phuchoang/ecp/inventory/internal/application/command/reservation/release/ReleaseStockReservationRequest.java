package org.phuchoang.ecp.inventory.internal.application.command.reservation.release;

import java.util.Objects;
import java.util.UUID;

/** Application input for releasing a held stock reservation. */
public record ReleaseStockReservationRequest(UUID stockItemId, UUID reservationId, boolean orderExists) {
    public ReleaseStockReservationRequest {
        Objects.requireNonNull(stockItemId, "stockItemId");
        Objects.requireNonNull(reservationId, "reservationId");
    }
}

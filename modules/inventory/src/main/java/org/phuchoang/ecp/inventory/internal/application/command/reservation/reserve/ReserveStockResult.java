package org.phuchoang.ecp.inventory.internal.application.command.reservation.reserve;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Application result containing the holds for one order reservation request. */
public record ReserveStockResult(UUID orderId, List<ReservedStock> reservations) {
    public ReserveStockResult {
        Objects.requireNonNull(orderId, "orderId");
        reservations = List.copyOf(Objects.requireNonNull(reservations, "reservations"));
    }
}

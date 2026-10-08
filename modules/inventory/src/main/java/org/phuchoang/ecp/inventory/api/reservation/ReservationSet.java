package org.phuchoang.ecp.inventory.api.reservation;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** The existing or newly-created reservations for one order placement. */
public record ReservationSet(UUID orderId, List<ReservationView> reservations) {
    public ReservationSet {
        Objects.requireNonNull(orderId, "orderId");
        reservations = List.copyOf(Objects.requireNonNull(reservations, "reservations"));
    }
}

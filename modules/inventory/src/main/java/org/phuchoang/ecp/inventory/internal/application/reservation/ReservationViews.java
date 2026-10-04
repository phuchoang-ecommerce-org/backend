package org.phuchoang.ecp.inventory.internal.application.reservation;

import org.phuchoang.ecp.inventory.api.reservation.ReservationActionResult;
import org.phuchoang.ecp.inventory.api.reservation.ReservationView;
import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;
import org.phuchoang.ecp.inventory.internal.domain.model.StockReservation;

import java.util.List;

/** Boundary mapping from Inventory's aggregate children to the integration representation. */
final class ReservationViews {

    private ReservationViews() {
    }

    static ReservationView of(StockItem item, StockReservation reservation) {
        return new ReservationView(reservation.id(), item.id(), reservation.orderId(), reservation.orderLineId(),
            reservation.quantity(), ReservationView.Status.valueOf(reservation.status().name()), reservation.expiresAt(),
            reservation.resolvedAt(), reservation.orphanedAt());
    }

    static List<ReservationView> forOrder(List<StockItem> items, java.util.UUID orderId) {
        return items.stream().flatMap(item -> item.reservations().stream()
                .filter(reservation -> reservation.orderId().equals(orderId)).map(reservation -> of(item, reservation)))
            .sorted(java.util.Comparator.comparing(ReservationView::orderLineId)
                .thenComparing(ReservationView::stockItemId).thenComparing(ReservationView::reservationId))
            .toList();
    }

    static ReservationActionResult.Outcome outcome(StockItem.TransitionOutcome outcome) {
        return ReservationActionResult.Outcome.valueOf(outcome.name());
    }
}

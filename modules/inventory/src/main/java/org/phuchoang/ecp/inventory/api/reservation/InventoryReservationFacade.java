package org.phuchoang.ecp.inventory.api.reservation;

import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.inventory.internal.application.reservation.CommitStockReservationService;
import org.phuchoang.ecp.inventory.internal.application.reservation.ReleaseStockReservationService;
import org.phuchoang.ecp.inventory.internal.application.reservation.ReserveStockService;
import org.springframework.stereotype.Component;

/**
 * Inventory's in-process boundary for stock state. It has no controller: Ordering owns the future
 * {@code StockReservationPort} adapter that will call this facade inside order placement.
 */
@Component
public final class InventoryReservationFacade {

    private final ReserveStockService reserve;
    private final ReleaseStockReservationService release;
    private final CommitStockReservationService commit;

    InventoryReservationFacade(ReserveStockService reserve, ReleaseStockReservationService release,
            CommitStockReservationService commit) {
        this.reserve = reserve;
        this.release = release;
        this.commit = commit;
    }

    public ReservationSet reserve(ReserveStockCommand command) {
        return reserve.reserve(command);
    }

    /** Releases a held reservation; {@code orderExists=false} records the required orphan investigation marker. */
    public ReservationActionResult release(ReservationReference reservation, boolean orderExists) {
        return release.release(reservation, orderExists);
    }

    /** Permission Matrix §5.4: only Warehouse and Administrator roles may commit a held reservation. */
    public ReservationActionResult commitStockReservation(IdentityActor caller, ReservationReference reservation) {
        return commit.commit(caller, reservation);
    }
}

package org.phuchoang.ecp.inventory.api.reservation;

import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.inventory.internal.application.reservation.command.commit.CommitStockReservationService;
import org.phuchoang.ecp.inventory.internal.application.reservation.command.release.ReleaseStockReservationService;
import org.phuchoang.ecp.inventory.internal.application.reservation.command.reserve.ReserveStockResult;
import org.phuchoang.ecp.inventory.internal.application.reservation.command.reserve.ReserveStockService;
import org.springframework.stereotype.Component;

/**
 * Inventory's in-process boundary for stock state. It has no controller: Ordering owns the future
 * {@code StockReservationPort} adapter that will call this facade inside order placement.
 */
@Component
public final class InventoryReservationFacade {

    private final ReserveStockService reserveStock;
    private final ReleaseStockReservationService releaseStockReservation;
    private final CommitStockReservationService commitStockReservation;
    private final InventoryReservationApiMapper mapper;

    InventoryReservationFacade(ReserveStockService reserve, ReleaseStockReservationService release,
            CommitStockReservationService commit, InventoryReservationApiMapper mapper) {
        this.reserveStock = reserve;
        this.releaseStockReservation = release;
        this.commitStockReservation = commit;
        this.mapper = mapper;
    }

    public ReservationSet reserve(ReserveStockCommand command) {
        try {
            ReserveStockResult result = reserveStock.reserve(mapper.reserveStockRequest(command));
            return mapper.reservationSet(result);
        } catch (org.phuchoang.ecp.inventory.internal.application.reservation.command.reserve.InsufficientStockException exception) {
            throw new InsufficientStockException(exception.shortfalls().stream()
                .map(shortfall -> new StockShortfall(shortfall.orderLineId(), shortfall.sku(),
                    shortfall.requestedQuantity(), shortfall.availableQuantity()))
                .toList());
        } catch (org.phuchoang.ecp.inventory.internal.application.reservation.command.reserve.UnpublishedProductException exception) {
            throw new UnpublishedProductException(exception.orderLineId());
        }
    }

    /** Releases a held reservation; {@code orderExists=false} records the required orphan investigation marker. */
    public ReservationActionResult release(ReservationReference reservation, boolean orderExists) {
        return mapper.actionResult(releaseStockReservation.release(mapper.releaseRequest(reservation, orderExists)));
    }

    /** Permission Matrix §5.4: only Warehouse and Administrator roles may commit a held reservation. */
    public ReservationActionResult commitStockReservation(IdentityActor caller, ReservationReference reservation) {
        return mapper.actionResult(commitStockReservation.commit(mapper.commitRequest(caller, reservation)));
    }
}

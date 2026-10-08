package org.phuchoang.ecp.inventory.api.reservation;

import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.inventory.internal.application.command.reservation.commit.CommitStockReservationRequest;
import org.phuchoang.ecp.inventory.internal.application.command.reservation.commit.CommitStockReservationResult;
import org.phuchoang.ecp.inventory.internal.application.command.reservation.commit.CommitStockReservationService;
import org.phuchoang.ecp.inventory.internal.application.command.reservation.release.ReleaseStockReservationRequest;
import org.phuchoang.ecp.inventory.internal.application.command.reservation.release.ReleaseStockReservationResult;
import org.phuchoang.ecp.inventory.internal.application.command.reservation.release.ReleaseStockReservationService;
import org.phuchoang.ecp.inventory.internal.application.command.reservation.reserve.ReserveStockRequest;
import org.phuchoang.ecp.inventory.internal.application.command.reservation.reserve.ReserveStockResult;
import org.phuchoang.ecp.inventory.internal.application.command.reservation.reserve.ReserveStockService;
import org.phuchoang.ecp.inventory.internal.application.command.reservation.reserve.ReservedStock;
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

    InventoryReservationFacade(ReserveStockService reserve, ReleaseStockReservationService release,
            CommitStockReservationService commit) {
        this.reserveStock = reserve;
        this.releaseStockReservation = release;
        this.commitStockReservation = commit;
    }

    public ReservationSet reserve(ReserveStockCommand command) {
        try {
            ReserveStockResult result = reserveStock.reserve(toApplicationRequest(command));
            return new ReservationSet(result.orderId(), result.reservations().stream()
                .map(InventoryReservationFacade::toApiView).toList());
        } catch (org.phuchoang.ecp.inventory.internal.application.command.reservation.reserve.InsufficientStockException exception) {
            throw new InsufficientStockException(exception.shortfalls().stream()
                .map(shortfall -> new StockShortfall(shortfall.orderLineId(), shortfall.sku(),
                    shortfall.requestedQuantity(), shortfall.availableQuantity()))
                .toList());
        } catch (org.phuchoang.ecp.inventory.internal.application.command.reservation.reserve.UnpublishedProductException exception) {
            throw new UnpublishedProductException(exception.orderLineId());
        }
    }

    /** Releases a held reservation; {@code orderExists=false} records the required orphan investigation marker. */
    public ReservationActionResult release(ReservationReference reservation, boolean orderExists) {
        return toApiResult(releaseStockReservation.release(new ReleaseStockReservationRequest(reservation.stockItemId(),
            reservation.reservationId(), orderExists)));
    }

    /** Permission Matrix §5.4: only Warehouse and Administrator roles may commit a held reservation. */
    public ReservationActionResult commitStockReservation(IdentityActor caller, ReservationReference reservation) {
        return toApiResult(commitStockReservation.commit(new CommitStockReservationRequest(caller,
            reservation.stockItemId(), reservation.reservationId())));
    }

    private static ReserveStockRequest toApplicationRequest(ReserveStockCommand command) {
        return new ReserveStockRequest(command.orderId(), command.lines().stream()
            .map(line -> new ReserveStockRequest.Line(line.orderLineId(), line.stockItemId(), line.sku(),
                line.quantity(), line.published()))
            .toList(), command.expiresAt());
    }

    private static ReservationActionResult toApiResult(ReleaseStockReservationResult result) {
        return new ReservationActionResult(toApiView(result), ReservationActionResult.Outcome.valueOf(result.outcome().name()));
    }

    private static ReservationActionResult toApiResult(CommitStockReservationResult result) {
        return new ReservationActionResult(toApiView(result), ReservationActionResult.Outcome.valueOf(result.outcome().name()));
    }

    private static ReservationView toApiView(ReservedStock result) {
        return new ReservationView(result.reservationId(), result.stockItemId(), result.orderId(), result.orderLineId(),
            result.quantity(), ReservationView.Status.valueOf(result.status().name()), result.expiresAt(),
            result.resolvedAt(), result.orphanedAt());
    }

    private static ReservationView toApiView(ReleaseStockReservationResult result) {
        return new ReservationView(result.reservationId(), result.stockItemId(), result.orderId(), result.orderLineId(),
            result.quantity(), ReservationView.Status.valueOf(result.status().name()), result.expiresAt(),
            result.resolvedAt(), result.orphanedAt());
    }

    private static ReservationView toApiView(CommitStockReservationResult result) {
        return new ReservationView(result.reservationId(), result.stockItemId(), result.orderId(), result.orderLineId(),
            result.quantity(), ReservationView.Status.valueOf(result.status().name()), result.expiresAt(),
            result.resolvedAt(), result.orphanedAt());
    }
}

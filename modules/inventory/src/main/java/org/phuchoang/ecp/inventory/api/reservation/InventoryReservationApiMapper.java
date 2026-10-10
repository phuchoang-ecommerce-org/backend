package org.phuchoang.ecp.inventory.api.reservation;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.inventory.internal.application.reservation.command.commit.CommitStockReservationRequest;
import org.phuchoang.ecp.inventory.internal.application.reservation.command.commit.CommitStockReservationResult;
import org.phuchoang.ecp.inventory.internal.application.reservation.command.release.ReleaseStockReservationRequest;
import org.phuchoang.ecp.inventory.internal.application.reservation.command.release.ReleaseStockReservationResult;
import org.phuchoang.ecp.inventory.internal.application.reservation.command.reserve.ReserveStockRequest;
import org.phuchoang.ecp.inventory.internal.application.reservation.command.reserve.ReserveStockResult;
import org.phuchoang.ecp.inventory.internal.application.reservation.command.reserve.ReservedStock;

/** Maps Inventory's published reservation contract to its application use cases. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface InventoryReservationApiMapper {

    ReserveStockRequest reserveStockRequest(ReserveStockCommand command);

    ReserveStockRequest.Line reserveStockLine(ReserveStockCommand.Line line);

    ReservationSet reservationSet(ReserveStockResult result);

    ReservationView reservationView(ReservedStock result);

    @Mapping(target = "stockItemId", source = "reservation.stockItemId")
    @Mapping(target = "reservationId", source = "reservation.reservationId")
    ReleaseStockReservationRequest releaseRequest(ReservationReference reservation, boolean orderExists);

    @Mapping(target = "caller", source = "caller")
    @Mapping(target = "stockItemId", source = "reservation.stockItemId")
    @Mapping(target = "reservationId", source = "reservation.reservationId")
    CommitStockReservationRequest commitRequest(IdentityActor caller, ReservationReference reservation);

    ReservationView reservationView(ReleaseStockReservationResult result);

    ReservationView reservationView(CommitStockReservationResult result);

    default ReservationActionResult actionResult(ReleaseStockReservationResult result) {
        return new ReservationActionResult(reservationView(result), ReservationActionResult.Outcome.valueOf(result.outcome().name()));
    }

    default ReservationActionResult actionResult(CommitStockReservationResult result) {
        return new ReservationActionResult(reservationView(result), ReservationActionResult.Outcome.valueOf(result.outcome().name()));
    }
}

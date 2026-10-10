package org.phuchoang.ecp.inventory.internal.application.reservation.command.commit;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;
import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;
import org.phuchoang.ecp.inventory.internal.domain.model.StockReservation;

/** Maps a committed reservation transition to its application result. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface CommitStockReservationMapper {

    @Mapping(target = "reservationId", source = "reservation.id")
    @Mapping(target = "stockItemId", source = "stockItem.id")
    @Mapping(target = "orderId", source = "reservation.orderId")
    @Mapping(target = "orderLineId", source = "reservation.orderLineId")
    @Mapping(target = "quantity", source = "reservation.quantity")
    @Mapping(target = "status", source = "reservation.status")
    @Mapping(target = "expiresAt", source = "reservation.expiresAt")
    @Mapping(target = "resolvedAt", source = "reservation.resolvedAt")
    @Mapping(target = "orphanedAt", source = "reservation.orphanedAt")
    @Mapping(target = "outcome", source = "outcome")
    CommitStockReservationResult result(StockItem stockItem, StockReservation reservation,
                                        StockItem.TransitionOutcome outcome);

    @ValueMapping(source = "RELEASED", target = MappingConstants.THROW_EXCEPTION)
    @ValueMapping(source = "ALREADY_RELEASED", target = MappingConstants.THROW_EXCEPTION)
    @ValueMapping(source = "DECLINED_ALREADY_COMMITTED", target = MappingConstants.THROW_EXCEPTION)
    CommitStockReservationResult.Outcome outcome(StockItem.TransitionOutcome outcome);
}

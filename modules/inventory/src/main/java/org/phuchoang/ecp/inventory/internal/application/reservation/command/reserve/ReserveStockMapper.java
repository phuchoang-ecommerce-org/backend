package org.phuchoang.ecp.inventory.internal.application.reservation.command.reserve;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;
import org.phuchoang.ecp.inventory.internal.domain.model.StockReservation;
import org.phuchoang.ecp.inventory.internal.domain.service.ReserveStockDomainService;

/** Maps reserve-stock application data to and from its domain request and result values. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface ReserveStockMapper {

    ReserveStockDomainService.ReservationRequest domainRequest(ReserveStockRequest request);

    ReserveStockDomainService.ReservationLine domainLine(ReserveStockRequest.Line line);

    @Mapping(target = "reservationId", source = "reservation.id")
    @Mapping(target = "stockItemId", source = "stockItem.id")
    @Mapping(target = "orderId", source = "reservation.orderId")
    @Mapping(target = "orderLineId", source = "reservation.orderLineId")
    @Mapping(target = "quantity", source = "reservation.quantity")
    @Mapping(target = "status", source = "reservation.status")
    @Mapping(target = "expiresAt", source = "reservation.expiresAt")
    @Mapping(target = "resolvedAt", source = "reservation.resolvedAt")
    @Mapping(target = "orphanedAt", source = "reservation.orphanedAt")
    ReservedStock reservedStock(StockItem stockItem, StockReservation reservation);
}

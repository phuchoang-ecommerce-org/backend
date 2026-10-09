package org.phuchoang.ecp.inventory.internal.application.reservation.command.reserve;

import org.phuchoang.ecp.inventory.internal.domain.service.ReserveStockDomainService;
import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;
import org.phuchoang.ecp.inventory.internal.domain.model.StockReservation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** One all-or-nothing local transaction for UC-INV-01. */
@Service
public class ReserveStockAttempt {

    private final ReserveStockDomainService reservations;

    public ReserveStockAttempt(ReserveStockDomainService reservations) {
        this.reservations = reservations;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReserveStockResult reserve(ReserveStockRequest command) {
        try {
            return reservationSet(command.orderId(), reservations.reserve(toDomainRequest(command)));
        } catch (ReserveStockDomainService.InsufficientStockException exception) {
            throw new InsufficientStockException(exception.shortfalls().stream()
                .map(shortfall -> new StockShortfall(shortfall.orderLineId(), skuFor(command, shortfall.orderLineId()),
                    shortfall.requestedQuantity(), shortfall.availableQuantity())).toList());
        } catch (ReserveStockDomainService.UnpublishedProductException exception) {
            throw new UnpublishedProductException(exception.orderLineId());
        }
    }

    private static ReserveStockDomainService.ReservationRequest toDomainRequest(ReserveStockRequest command) {
        return new ReserveStockDomainService.ReservationRequest(command.orderId(), command.lines().stream()
            .map(line -> new ReserveStockDomainService.ReservationLine(line.orderLineId(), line.stockItemId(),
                line.quantity(), line.published())).toList(), command.expiresAt());
    }

    private static String skuFor(ReserveStockRequest command, UUID orderLineId) {
        return command.lines().stream().filter(line -> line.orderLineId().equals(orderLineId)).findFirst()
            .map(ReserveStockRequest.Line::sku).orElseThrow();
    }

    private static ReserveStockResult reservationSet(UUID orderId, List<StockItem> items) {
        return new ReserveStockResult(orderId, items.stream().flatMap(item -> item.reservations().stream()
                .filter(reservation -> reservation.orderId().equals(orderId)).map(reservation -> toResult(item, reservation)))
            .sorted(Comparator.comparing(ReservedStock::orderLineId).thenComparing(ReservedStock::stockItemId)
                .thenComparing(ReservedStock::reservationId)).toList());
    }

    private static ReservedStock toResult(StockItem item, StockReservation reservation) {
        return new ReservedStock(reservation.id(), item.id(), reservation.orderId(), reservation.orderLineId(),
            reservation.quantity(), ReservedStock.Status.valueOf(reservation.status().name()), reservation.expiresAt(),
            reservation.resolvedAt(), reservation.orphanedAt());
    }
}

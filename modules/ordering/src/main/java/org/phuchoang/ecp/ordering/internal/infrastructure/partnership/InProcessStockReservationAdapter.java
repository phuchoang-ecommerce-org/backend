package org.phuchoang.ecp.ordering.internal.infrastructure.partnership;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.inventory.api.reservation.InventoryReservationFacade;
import org.phuchoang.ecp.inventory.api.reservation.ReserveStockCommand;
import org.phuchoang.ecp.ordering.internal.application.place.command.StockReservationPort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Current in-process implementation of Ordering's port; it can be replaced with a saga adapter on extraction. */
@Component
@RequiredArgsConstructor
class InProcessStockReservationAdapter implements StockReservationPort {
    private final InventoryReservationFacade inventory;

    @Override
    public List<Reservation> reserve(UUID orderId, List<Line> lines, Instant expiresAt) {
        return inventory.reserve(new ReserveStockCommand(orderId, lines.stream()
            .map(line -> new ReserveStockCommand.Line(line.orderLineId(), line.stockItemId(), line.sku(), line.quantity(), true))
            .toList(), expiresAt)).reservations().stream()
            .map(value -> new Reservation(value.orderLineId(), value.stockItemId(), value.warehouseId(), value.reservationId(), value.quantity()))
            .toList();
    }
}

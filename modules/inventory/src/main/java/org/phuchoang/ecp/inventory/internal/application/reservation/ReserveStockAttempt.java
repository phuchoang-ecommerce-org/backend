package org.phuchoang.ecp.inventory.internal.application.reservation;

import org.phuchoang.ecp.inventory.api.reservation.InsufficientStockException;
import org.phuchoang.ecp.inventory.api.reservation.ReservationSet;
import org.phuchoang.ecp.inventory.api.reservation.ReserveStockCommand;
import org.phuchoang.ecp.inventory.api.reservation.StockShortfall;
import org.phuchoang.ecp.inventory.api.reservation.UnpublishedProductException;
import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;
import org.phuchoang.ecp.inventory.internal.domain.repository.StockItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** One all-or-nothing local transaction for UC-INV-01. */
@Service
public class ReserveStockAttempt {

    private final StockItemRepository stockItems;

    public ReserveStockAttempt(StockItemRepository stockItems) {
        this.stockItems = stockItems;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReservationSet reserve(ReserveStockCommand command) {
        List<StockItem> existing = stockItems.findByOrderId(command.orderId());
        if (!existing.isEmpty()) {
            return new ReservationSet(command.orderId(), ReservationViews.forOrder(existing, command.orderId()));
        }
        command.lines().stream().filter(line -> !line.published()).findFirst()
            .ifPresent(line -> { throw new UnpublishedProductException(line.orderLineId()); });

        Map<UUID, StockItem> items = new LinkedHashMap<>();
        stockItems.findByIds(command.lines().stream().map(ReserveStockCommand.Line::stockItemId).distinct()
            .sorted(Comparator.naturalOrder()).toList()).forEach(item -> items.put(item.id(), item));

        Map<UUID, StockItem> changed = new LinkedHashMap<>();
        List<StockShortfall> shortfalls = new ArrayList<>();
        for (ReserveStockCommand.Line line : command.lines()) {
            StockItem item = changed.getOrDefault(line.stockItemId(), items.get(line.stockItemId()));
            if (item == null || item.availableQuantity() < line.quantity()) {
                shortfalls.add(new StockShortfall(line.orderLineId(), line.sku(), line.quantity(),
                    item == null ? 0 : item.availableQuantity()));
                continue;
            }
            changed.put(item.id(), item.reserve(UUID.randomUUID(), command.orderId(), line.orderLineId(),
                line.quantity(), command.expiresAt()));
        }
        if (!shortfalls.isEmpty()) {
            throw new InsufficientStockException(shortfalls);
        }
        stockItems.saveAll(changed.values());
        return new ReservationSet(command.orderId(), ReservationViews.forOrder(List.copyOf(changed.values()), command.orderId()));
    }
}

package org.phuchoang.ecp.inventory.internal.application.command.reservation.reserve;

import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;
import org.phuchoang.ecp.inventory.internal.domain.model.StockReservation;
import org.phuchoang.ecp.inventory.internal.domain.repository.StockItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** One all-or-nothing local transaction for UC-INV-01. */
@Service
public class ReserveStockAttempt {

    private final StockItemRepository stockItems;

    public ReserveStockAttempt(StockItemRepository stockItems) {
        this.stockItems = stockItems;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReserveStockResult reserve(ReserveStockRequest command) {
        return existingReservations(command).orElseGet(() -> reserveNew(command));
    }

    private Optional<ReserveStockResult> existingReservations(ReserveStockRequest command) {
        List<StockItem> existing = stockItems.findByOrderId(command.orderId());
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(reservationSet(command.orderId(), existing));
    }

    private ReserveStockResult reserveNew(ReserveStockRequest command) {
        assertAllProductsPublished(command);
        ReservationChanges changes = reserveLines(command, stockItemsById(command));
        throwIfInsufficient(changes.shortfalls());
        stockItems.saveAll(changes.stockItems().values());
        return reservationSet(command.orderId(), List.copyOf(changes.stockItems().values()));
    }

    private static void assertAllProductsPublished(ReserveStockRequest command) {
        command.lines().stream().filter(line -> !line.published()).findFirst()
            .ifPresent(line -> { throw new UnpublishedProductException(line.orderLineId()); });
    }

    private Map<UUID, StockItem> stockItemsById(ReserveStockRequest command) {
        Map<UUID, StockItem> items = new LinkedHashMap<>();
        stockItems.findByIds(command.lines().stream().map(ReserveStockRequest.Line::stockItemId).distinct()
            .sorted(Comparator.naturalOrder()).toList()).forEach(item -> items.put(item.id(), item));
        return items;
    }

    private static ReservationChanges reserveLines(ReserveStockRequest command, Map<UUID, StockItem> stockItems) {
        Map<UUID, StockItem> changed = new LinkedHashMap<>();
        List<StockShortfall> shortfalls = new ArrayList<>();
        for (ReserveStockRequest.Line line : command.lines()) {
            StockItem item = changed.getOrDefault(line.stockItemId(), stockItems.get(line.stockItemId()));
            if (item == null || item.availableQuantity() < line.quantity()) {
                shortfalls.add(new StockShortfall(line.orderLineId(), line.sku(), line.quantity(),
                    item == null ? 0 : item.availableQuantity()));
                continue;
            }
            changed.put(item.id(), item.reserve(UUID.randomUUID(), command.orderId(), line.orderLineId(),
                line.quantity(), command.expiresAt()));
        }
        return new ReservationChanges(changed, shortfalls);
    }

    private static void throwIfInsufficient(List<StockShortfall> shortfalls) {
        if (!shortfalls.isEmpty()) {
            throw new InsufficientStockException(shortfalls);
        }
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

    /** Private workflow state; it has no contract beyond this one transactional attempt. */
    private record ReservationChanges(Map<UUID, StockItem> stockItems, List<StockShortfall> shortfalls) { }
}

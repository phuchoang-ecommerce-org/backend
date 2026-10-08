package org.phuchoang.ecp.inventory.internal.domain.service;

import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;
import org.phuchoang.ecp.inventory.internal.domain.repository.StockItemRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Executes the authoritative all-or-nothing reservation transition for StockItem aggregates. */
public final class ReserveStockDomainService {

    private final StockItemRepository stockItems;

    public ReserveStockDomainService(StockItemRepository stockItems) {
        this.stockItems = stockItems;
    }

    public List<StockItem> reserve(ReservationRequest request) {
        return existingReservations(request.orderId()).orElseGet(() -> reserveNew(request));
    }

    private Optional<List<StockItem>> existingReservations(UUID orderId) {
        List<StockItem> existing = stockItems.findByOrderId(orderId);
        return existing.isEmpty() ? Optional.empty() : Optional.of(existing);
    }

    private List<StockItem> reserveNew(ReservationRequest request) {
        assertAllProductsPublished(request);
        ReservationChanges changes = reserveLines(request, stockItemsById(request.lines()));
        throwIfInsufficient(changes.shortfalls());
        stockItems.saveAll(changes.stockItems().values());
        return List.copyOf(changes.stockItems().values());
    }

    private static void assertAllProductsPublished(ReservationRequest request) {
        request.lines().stream().filter(line -> !line.published()).findFirst()
            .ifPresent(line -> { throw new UnpublishedProductException(line.orderLineId()); });
    }

    private Map<UUID, StockItem> stockItemsById(List<ReservationLine> lines) {
        Map<UUID, StockItem> items = new LinkedHashMap<>();
        stockItems.findByIds(lines.stream().map(ReservationLine::stockItemId).distinct().sorted().toList())
            .forEach(item -> items.put(item.id(), item));
        return items;
    }

    private static ReservationChanges reserveLines(ReservationRequest request, Map<UUID, StockItem> stockItems) {
        Map<UUID, StockItem> changed = new LinkedHashMap<>();
        List<StockShortfall> shortfalls = new ArrayList<>();
        for (ReservationLine line : request.lines()) {
            StockItem item = changed.getOrDefault(line.stockItemId(), stockItems.get(line.stockItemId()));
            if (item == null || item.availableQuantity() < line.quantity()) {
                shortfalls.add(new StockShortfall(line.orderLineId(), line.stockItemId(), line.quantity(),
                    item == null ? 0 : item.availableQuantity()));
                continue;
            }
            changed.put(item.id(), item.reserve(UUID.randomUUID(), request.orderId(), line.orderLineId(), line.quantity(),
                request.expiresAt()));
        }
        return new ReservationChanges(changed, shortfalls);
    }

    private static void throwIfInsufficient(List<StockShortfall> shortfalls) {
        if (!shortfalls.isEmpty()) {
            throw new InsufficientStockException(shortfalls);
        }
    }

    public record ReservationRequest(UUID orderId, List<ReservationLine> lines, Instant expiresAt) {
        public ReservationRequest {
            if (orderId == null || expiresAt == null) {
                throw new IllegalArgumentException("orderId and expiresAt are required.");
            }
            lines = List.copyOf(lines);
            if (lines.isEmpty()) {
                throw new IllegalArgumentException("A stock reservation needs at least one line.");
            }
        }
    }

    public record ReservationLine(UUID orderLineId, UUID stockItemId, int quantity, boolean published) {
        public ReservationLine {
            if (orderLineId == null || stockItemId == null || quantity <= 0) {
                throw new IllegalArgumentException("A reservation line is invalid.");
            }
        }
    }

    public record StockShortfall(UUID orderLineId, UUID stockItemId, int requestedQuantity, int availableQuantity) { }

    public static final class InsufficientStockException extends RuntimeException {
        private final List<StockShortfall> shortfalls;

        public InsufficientStockException(Collection<StockShortfall> shortfalls) {
            this.shortfalls = List.copyOf(shortfalls);
        }

        public List<StockShortfall> shortfalls() {
            return shortfalls;
        }
    }

    public static final class UnpublishedProductException extends RuntimeException {
        private final UUID orderLineId;

        public UnpublishedProductException(UUID orderLineId) {
            this.orderLineId = orderLineId;
        }

        public UUID orderLineId() {
            return orderLineId;
        }
    }

    private record ReservationChanges(Map<UUID, StockItem> stockItems, List<StockShortfall> shortfalls) { }
}

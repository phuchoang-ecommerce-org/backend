package org.phuchoang.ecp.inventory.internal.domain.service;

import org.phuchoang.ecp.inventory.internal.domain.model.InsufficientAvailableStockException;
import org.phuchoang.ecp.inventory.internal.domain.model.StockAdjustment;
import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;
import org.phuchoang.ecp.inventory.internal.domain.repository.StockItemRepository;

import java.time.Clock;
import java.util.UUID;

/** Authoritative aggregate transition for a physical stock adjustment. */
public final class AdjustStockDomainService {
    private final StockItemRepository stockItems;
    private final Clock clock;

    public AdjustStockDomainService(StockItemRepository stockItems, Clock clock) {
        this.stockItems = stockItems;
        this.clock = clock;
    }

    public Adjustment adjust(UUID stockItemId, StockAdjustment proposedAdjustment) {
        StockItem before = stockItems.findById(stockItemId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown stock item."));
        StockItem.Adjustment rootAdjustment;
        try {
            rootAdjustment = before.adjust(proposedAdjustment, UUID.randomUUID(), clock.instant());
        } catch (InsufficientAvailableStockException exception) {
            throw new AdjustmentDeclinedException(before);
        }
        stockItems.save(rootAdjustment);
        return new Adjustment(before, rootAdjustment.stockItem(), rootAdjustment.stockAdjustment());
    }

    public record Adjustment(StockItem before, StockItem after, StockAdjustment stockAdjustment) { }

    /** Carries the authoritative held reservations needed for the application failure result. */
    public static final class AdjustmentDeclinedException extends RuntimeException {
        private final StockItem stockItem;

        AdjustmentDeclinedException(StockItem stockItem) {
            this.stockItem = stockItem;
        }

        public StockItem stockItem() {
            return stockItem;
        }
    }
}

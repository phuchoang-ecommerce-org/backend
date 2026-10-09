package org.phuchoang.ecp.inventory.internal.domain.service;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.inventory.internal.domain.model.StockAdjustment;
import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;
import org.phuchoang.ecp.inventory.internal.domain.repository.StockItemRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdjustStockDomainServiceTest {

    private final StockItemRepository stockItems = mock(StockItemRepository.class);
    private final AdjustStockDomainService adjustments = new AdjustStockDomainService(stockItems,
        Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void persistsTheAggregateAdjustmentAndRetainsHeldUnits() {
        UUID stockItemId = UUID.randomUUID();
        StockItem held = StockItem.open(stockItemId, "SKU-1", UUID.randomUUID(), 5)
            .reserve(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 3,
                java.time.Instant.parse("2030-02-01T00:00:00Z"));
        when(stockItems.findById(stockItemId)).thenReturn(Optional.of(held));

        AdjustStockDomainService.Adjustment adjustment = adjustments.adjust(stockItemId, draft(4));

        assertThat(adjustment.after().quantityOnHand()).isEqualTo(9);
        assertThat(adjustment.after().quantityReserved()).isEqualTo(3);
        verify(stockItems).save(new StockItem.Adjustment(adjustment.after(), adjustment.stockAdjustment()));
    }

    @Test
    void exposesTheAuthoritativeHeldReservationsWhenAnAdjustmentIsDeclined() {
        UUID stockItemId = UUID.randomUUID();
        StockItem held = StockItem.open(stockItemId, "SKU-1", UUID.randomUUID(), 5)
            .reserve(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 3,
                java.time.Instant.parse("2030-02-01T00:00:00Z"));
        when(stockItems.findById(stockItemId)).thenReturn(Optional.of(held));

        assertThatThrownBy(() -> adjustments.adjust(stockItemId, draft(-3)))
            .isInstanceOf(AdjustStockDomainService.AdjustmentDeclinedException.class)
            .satisfies(exception -> assertThat(((AdjustStockDomainService.AdjustmentDeclinedException) exception)
                .stockItem()).isEqualTo(held));
        verify(stockItems, never()).saveAll(any());
        verify(stockItems, never()).save(any());
    }

    private static StockAdjustment draft(int delta) {
        return StockAdjustment.proposed(delta, "COUNT", "Cycle count", UUID.randomUUID());
    }
}

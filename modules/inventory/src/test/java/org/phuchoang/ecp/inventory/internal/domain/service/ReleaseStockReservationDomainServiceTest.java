package org.phuchoang.ecp.inventory.internal.domain.service;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;
import org.phuchoang.ecp.inventory.internal.domain.repository.StockItemRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReleaseStockReservationDomainServiceTest {

    @Test
    void releasesTheAggregateTransitionAndPersistsTheChangedAggregate() {
        StockItemRepository stockItems = mock(StockItemRepository.class);
        ReleaseStockReservationDomainService service = new ReleaseStockReservationDomainService(stockItems,
            Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC));
        UUID stockItemId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        StockItem held = StockItem.open(stockItemId, "SKU-1", UUID.randomUUID(), 3)
            .reserve(reservationId, UUID.randomUUID(), UUID.randomUUID(), 2, Instant.parse("2030-02-01T00:00:00Z"));
        when(stockItems.findById(stockItemId)).thenReturn(Optional.of(held));

        StockItem.Transition transition = service.release(stockItemId, reservationId, true);

        assertThat(transition.outcome()).isEqualTo(StockItem.TransitionOutcome.RELEASED);
        assertThat(transition.stockItem().quantityOnHand()).isEqualTo(3);
        assertThat(transition.stockItem().quantityReserved()).isZero();
        assertThat(transition.reservation().orphanedAt()).isEqualTo(Instant.parse("2030-01-01T00:00:00Z"));
        verify(stockItems).saveAll(any());
    }
}

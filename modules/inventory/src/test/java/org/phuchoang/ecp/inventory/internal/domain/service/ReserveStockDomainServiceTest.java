package org.phuchoang.ecp.inventory.internal.domain.service;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.inventory.internal.domain.model.ReservationStatus;
import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;
import org.phuchoang.ecp.inventory.internal.domain.repository.StockItemRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReserveStockDomainServiceTest {

    private final StockItemRepository stockItems = mock(StockItemRepository.class);
    private final ReserveStockDomainService reservations = new ReserveStockDomainService(stockItems);

    @Test
    void loadsAndSavesTheAuthoritativeAggregateAfterAValidReservation() {
        UUID stockItemId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID orderLineId = UUID.randomUUID();
        when(stockItems.findByOrderId(orderId)).thenReturn(List.of());
        when(stockItems.findByIds(List.of(stockItemId))).thenReturn(List.of(StockItem.open(stockItemId, "SKU-1",
            UUID.randomUUID(), 3)));

        List<StockItem> changed = reservations.reserve(request(orderId, orderLineId, stockItemId, 2, true));

        assertThat(changed).singleElement().satisfies(item -> {
            assertThat(item.quantityReserved()).isEqualTo(2);
            assertThat(item.reservations()).singleElement().satisfies(reservation -> {
                assertThat(reservation.orderId()).isEqualTo(orderId);
                assertThat(reservation.status()).isEqualTo(ReservationStatus.HELD);
            });
        });
        verify(stockItems).saveAll(any());
    }

    @Test
    void rejectsInsufficientStockBeforePersistingAnyAggregate() {
        UUID stockItemId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        when(stockItems.findByOrderId(orderId)).thenReturn(List.of());
        when(stockItems.findByIds(List.of(stockItemId))).thenReturn(List.of(StockItem.open(stockItemId, "SKU-1",
            UUID.randomUUID(), 1)));

        assertThatThrownBy(() -> reservations.reserve(request(orderId, UUID.randomUUID(), stockItemId, 2, true)))
            .isInstanceOf(ReserveStockDomainService.InsufficientStockException.class);

        verify(stockItems, org.mockito.Mockito.never()).saveAll(any());
    }

    @Test
    void preservesIdempotencyByReturningExistingOrderReservations() {
        UUID stockItemId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        StockItem existing = StockItem.open(stockItemId, "SKU-1", UUID.randomUUID(), 3)
            .reserve(UUID.randomUUID(), orderId, UUID.randomUUID(), 1, Instant.parse("2030-02-01T00:00:00Z"));
        when(stockItems.findByOrderId(orderId)).thenReturn(List.of(existing));

        assertThat(reservations.reserve(request(orderId, UUID.randomUUID(), stockItemId, 1, true)))
            .containsExactly(existing);
        verify(stockItems, org.mockito.Mockito.never()).saveAll(any());
    }

    private static ReserveStockDomainService.ReservationRequest request(UUID orderId, UUID orderLineId,
            UUID stockItemId, int quantity, boolean published) {
        return new ReserveStockDomainService.ReservationRequest(orderId,
            List.of(new ReserveStockDomainService.ReservationLine(orderLineId, stockItemId, quantity, published)),
            Instant.parse("2030-02-01T00:00:00Z"));
    }
}

package org.phuchoang.ecp.inventory.internal.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockItemTest {

    private static final Instant NOW = Instant.parse("2026-10-04T10:00:00Z");

    @Test
    void reservesThenReleasesExactlyOnce() {
        StockItem item = stock(3);
        UUID reservationId = UUID.randomUUID();
        StockItem held = item.reserve(reservationId, UUID.randomUUID(), UUID.randomUUID(), 2, NOW.plusSeconds(300));

        StockItem.Transition released = held.release(reservationId, NOW, false);
        StockItem.Transition repeated = released.stockItem().release(reservationId, NOW.plusSeconds(1), false);

        assertThat(held.availableQuantity()).isEqualTo(1);
        assertThat(released.stockItem().availableQuantity()).isEqualTo(3);
        assertThat(released.outcome()).isEqualTo(StockItem.TransitionOutcome.RELEASED);
        assertThat(repeated.outcome()).isEqualTo(StockItem.TransitionOutcome.ALREADY_RELEASED);
        assertThat(repeated.stockItem().quantityReserved()).isZero();
    }

    @Test
    void commitsExactlyOnceAndDeclinesAReleasedReservation() {
        UUID reservationId = UUID.randomUUID();
        StockItem held = stock(3).reserve(reservationId, UUID.randomUUID(), UUID.randomUUID(), 2, NOW.plusSeconds(300));

        StockItem.Transition committed = held.commit(reservationId, NOW);
        StockItem.Transition repeated = committed.stockItem().commit(reservationId, NOW.plusSeconds(1));
        StockItem.Transition releasedThenCommitted = held.release(reservationId, NOW, false)
            .stockItem().commit(reservationId, NOW.plusSeconds(1));

        assertThat(committed.stockItem().quantityOnHand()).isEqualTo(1);
        assertThat(committed.stockItem().availableQuantity()).isEqualTo(1);
        assertThat(repeated.outcome()).isEqualTo(StockItem.TransitionOutcome.ALREADY_COMMITTED);
        assertThat(releasedThenCommitted.outcome()).isEqualTo(StockItem.TransitionOutcome.DECLINED_ALREADY_RELEASED);
    }

    @Test
    void refusesToReserveMoreThanCurrentAvailability() {
        assertThatThrownBy(() -> stock(1).reserve(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 2,
            NOW.plusSeconds(300)))
            .isInstanceOf(InsufficientAvailableStockException.class)
            .extracting(error -> ((InsufficientAvailableStockException) error).availableQuantity())
            .isEqualTo(1);
    }

    private static StockItem stock(int quantity) {
        return StockItem.open(UUID.randomUUID(), "SKU-1", UUID.randomUUID(), quantity);
    }
}

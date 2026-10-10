package org.phuchoang.ecp.inventory.internal.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.phuchoang.ecp.inventory.internal.domain.model.StockAdjustment;
import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;
import org.phuchoang.ecp.inventory.internal.domain.model.StockReservation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryStockItemEntityTest {

    private final StockItemJpaMapper mapper = Mappers.getMapper(StockItemJpaMapper.class);

    @Test
    void mapsTheAuthoritativeAggregateAndItsOwnedReservations() {
        Instant now = Instant.parse("2026-10-09T00:00:00Z");
        UUID stockItemId = UUID.randomUUID();
        StockReservation reservation = StockReservation.held(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 2,
            now.plusSeconds(3600));
        StockItem item = new StockItem(stockItemId, "SKU-1", UUID.randomUUID(), UUID.randomUUID(), 10, 2, 0,
            List.of(reservation));

        InventoryStockItemEntity entity = mapper.toEntity(item, now);

        assertThat(entity.getCreatedAt()).isEqualTo(now);
        assertThat(entity.getUpdatedAt()).isEqualTo(now);
        assertThat(entity.getReservations()).singleElement().satisfies(value -> {
            assertThat(value.getStockItem()).isSameAs(entity);
            assertThat(value.getCreatedAt()).isEqualTo(now);
        });
        assertThat(mapper.toDomain(entity)).isEqualTo(item);

        InventoryStockAdjustmentEntity adjustment = mapper.toEntity(new StockAdjustment(UUID.randomUUID(), stockItemId,
            3, "COUNT", "Cycle count", UUID.randomUUID(), now));
        assertThat(adjustment).extracting(InventoryStockAdjustmentEntity::getStockItemId,
            InventoryStockAdjustmentEntity::getDelta).containsExactly(stockItemId, 3);
    }

    @Test
    void synchronizesOwnedReservationsWithoutRetainingRemovedChildren() {
        Instant createdAt = Instant.parse("2026-10-09T00:00:00Z");
        UUID stockItemId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        InventoryStockItemEntity stockItem = new InventoryStockItemEntity();
        stockItem.setId(stockItemId);
        stockItem.setSku("SKU-1");
        stockItem.setWarehouseId(UUID.randomUUID());
        stockItem.setQuantityOnHand(10);
        stockItem.setQuantityReserved(0);
        stockItem.initializeAuditTimestamps(createdAt);
        InventoryStockReservationEntity held = reservation(reservationId, UUID.randomUUID(), UUID.randomUUID(), 2,
            "HELD", createdAt.plusSeconds(3600), null, null, createdAt, createdAt);

        stockItem.setQuantityOnHand(10);
        stockItem.setQuantityReserved(2);
        stockItem.synchronizeReservations(List.of(held), createdAt);
        InventoryStockReservationEntity released = reservation(reservationId, held.getOrderId(), held.getOrderLineId(), 2,
            "RELEASED", held.getExpiresAt(), createdAt.plusSeconds(60), createdAt.plusSeconds(60), createdAt,
            createdAt.plusSeconds(60));
        stockItem.setQuantityOnHand(8);
        stockItem.setQuantityReserved(0);
        stockItem.synchronizeReservations(List.of(released), createdAt.plusSeconds(60));

        assertThat(stockItem.getQuantityOnHand()).isEqualTo(8);
        assertThat(stockItem.getQuantityReserved()).isZero();
        assertThat(stockItem.getReservations()).singleElement().satisfies(reservation -> {
            assertThat(reservation.getStatus()).isEqualTo("RELEASED");
            assertThat(reservation.getResolvedAt()).isEqualTo(createdAt.plusSeconds(60));
            assertThat(reservation.getOrphanedAt()).isEqualTo(createdAt.plusSeconds(60));
        });

        stockItem.synchronizeReservations(List.of(), createdAt.plusSeconds(120));

        assertThat(stockItem.getReservations()).isEmpty();
    }

    private static InventoryStockReservationEntity reservation(UUID id, UUID orderId, UUID orderLineId, int quantity,
                                                                String status, Instant expiresAt, Instant resolvedAt,
                                                                Instant orphanedAt, Instant createdAt, Instant updatedAt) {
        InventoryStockReservationEntity reservation = new InventoryStockReservationEntity();
        reservation.setId(id);
        reservation.setOrderId(orderId);
        reservation.setOrderLineId(orderLineId);
        reservation.setQuantity(quantity);
        reservation.setStatus(status);
        reservation.setExpiresAt(expiresAt);
        reservation.setResolvedAt(resolvedAt);
        reservation.setOrphanedAt(orphanedAt);
        reservation.initializeAuditTimestamps(createdAt, updatedAt);
        return reservation;
    }
}

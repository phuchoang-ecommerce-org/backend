package org.phuchoang.ecp.inventory.internal.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import org.phuchoang.ecp.inventory.internal.domain.model.ReservationStatus;
import org.phuchoang.ecp.inventory.internal.domain.model.StockAdjustment;
import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;
import org.phuchoang.ecp.inventory.internal.domain.model.StockReservation;
import org.phuchoang.ecp.inventory.internal.domain.repository.StockItemRepository;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** PostgreSQL adapter for the StockItem aggregate; {@code @Version} is enforced on every save. */
@Repository
class JpaStockItemRepositoryAdapter implements StockItemRepository {

    private final InventoryStockItemJpaRepository stockItems;
    private final EntityManager entityManager;
    private final Clock clock;

    JpaStockItemRepositoryAdapter(InventoryStockItemJpaRepository stockItems, EntityManager entityManager, Clock clock) {
        this.stockItems = stockItems;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Override
    public Optional<StockItem> findById(UUID stockItemId) {
        return stockItems.findAggregateById(stockItemId).map(JpaStockItemRepositoryAdapter::toDomain);
    }

    @Override
    public List<StockItem> findByIds(Collection<UUID> stockItemIds) {
        return stockItems.findAllByIdIn(stockItemIds).stream().map(JpaStockItemRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<StockItem> findByOrderId(UUID orderId) {
        return stockItems.findAggregatesByOrderId(orderId).stream().map(JpaStockItemRepositoryAdapter::toDomain).toList();
    }

    @Override
    public void saveAll(Collection<StockItem> aggregates) {
        synchronize(aggregates);
        // Surface an optimistic-lock conflict inside the attempt transaction so the caller can retry it.
        stockItems.flush();
    }

    @Override
    public void save(StockItem.Adjustment adjustment) {
        synchronize(List.of(adjustment.stockItem()));
        entityManager.persist(toEntity(adjustment.stockAdjustment()));
        // The root update and its immutable child must conflict or commit together.
        stockItems.flush();
    }

    private void synchronize(Collection<StockItem> aggregates) {
        Map<UUID, InventoryStockItemEntity> existing = stockItems.findAllByIdIn(aggregates.stream().map(StockItem::id)
            .toList()).stream().collect(Collectors.toMap(InventoryStockItemEntity::id, Function.identity()));
        Instant now = clock.instant();
        List<InventoryStockItemEntity> entities = aggregates.stream().map(aggregate -> {
            InventoryStockItemEntity entity = existing.get(aggregate.id());
            List<InventoryStockReservationEntity> reservations = aggregate.reservations().stream()
                .map(reservation -> toEntity(reservation, now)).toList();
            if (entity == null) {
                entity = new InventoryStockItemEntity(aggregate.id(), aggregate.sku(), aggregate.warehouseId(),
                    aggregate.ownerId(), aggregate.quantityOnHand(), aggregate.quantityReserved(), now);
            }
            entity.synchronize(aggregate.quantityOnHand(), aggregate.quantityReserved(), reservations, now);
            return entity;
        }).toList();
        stockItems.saveAll(entities);
    }

    private static StockItem toDomain(InventoryStockItemEntity entity) {
        return new StockItem(entity.id(), entity.sku(), entity.warehouseId(), entity.ownerId(), entity.quantityOnHand(),
            entity.quantityReserved(), entity.version(), entity.reservations().stream()
                .map(JpaStockItemRepositoryAdapter::toDomain).toList());
    }

    private static StockReservation toDomain(InventoryStockReservationEntity entity) {
        return new StockReservation(entity.id(), entity.orderId(), entity.orderLineId(), entity.quantity(),
            ReservationStatus.valueOf(entity.status()), entity.expiresAt(), entity.resolvedAt(), entity.orphanedAt());
    }

    private static InventoryStockReservationEntity toEntity(StockReservation reservation, Instant now) {
        Instant createdAt = reservation.resolvedAt() == null ? now : reservation.resolvedAt();
        // Existing rows retain their database creation timestamp; it is not part of the domain state.
        return new InventoryStockReservationEntity(reservation.id(), reservation.orderId(), reservation.orderLineId(),
            reservation.quantity(), reservation.status().name(), reservation.expiresAt(), reservation.resolvedAt(),
            reservation.orphanedAt(), createdAt, now);
    }

    private static InventoryStockAdjustmentEntity toEntity(StockAdjustment adjustment) {
        return new InventoryStockAdjustmentEntity(adjustment.id(), adjustment.stockItemId(), adjustment.delta(),
            adjustment.reasonCode(), adjustment.reason(), adjustment.actorId(), adjustment.occurredAt());
    }
}

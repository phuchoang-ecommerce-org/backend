package org.phuchoang.ecp.inventory.internal.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import org.phuchoang.ecp.inventory.internal.domain.model.StockAdjustment;
import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;
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

/**
 * PostgreSQL adapter for the StockItem aggregate; {@code @Version} is enforced
 * on every save.
 */
@Repository
class JpaStockItemRepositoryAdapter implements StockItemRepository {

  private final InventoryStockItemJpaRepository stockItems;
  private final EntityManager entityManager;
  private final Clock clock;
  private final StockItemJpaMapper mapper;

  JpaStockItemRepositoryAdapter(InventoryStockItemJpaRepository stockItems, EntityManager entityManager, Clock clock,
      StockItemJpaMapper mapper) {
    this.stockItems = stockItems;
    this.entityManager = entityManager;
    this.clock = clock;
    this.mapper = mapper;
  }

  @Override
  public Optional<StockItem> findById(UUID stockItemId) {
    return stockItems.findAggregateById(stockItemId).map(mapper::toDomain);
  }

  @Override
  public List<StockItem> findByIds(Collection<UUID> stockItemIds) {
    return stockItems.findAllByIdIn(stockItemIds).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<StockItem> findByOrderId(UUID orderId) {
    return stockItems.findAggregatesByOrderId(orderId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public void saveAll(Collection<StockItem> aggregates) {
    synchronize(aggregates);
    // Surface an optimistic-lock conflict inside the attempt transaction so the
    // caller can retry it.
    stockItems.flush();
  }

  @Override
  public void save(StockItem.Adjustment adjustment) {
    synchronize(List.of(adjustment.stockItem()));
    entityManager.persist(mapper.toEntity(adjustment.stockAdjustment()));
    // The root update and its immutable child must conflict or commit together.
    stockItems.flush();
  }

  private void synchronize(Collection<StockItem> aggregates) {
    Map<UUID, InventoryStockItemEntity> existing = stockItems.findAllByIdIn(aggregates.stream().map(StockItem::id)
        .toList()).stream().collect(Collectors.toMap(InventoryStockItemEntity::getId, Function.identity()));
    Instant now = clock.instant();
    List<InventoryStockItemEntity> entities = aggregates.stream().map(aggregate -> {
      InventoryStockItemEntity entity = existing.get(aggregate.id());
      if (entity == null) {
        entity = mapper.toEntity(aggregate, now);
      } else {
        mapper.updateEntity(aggregate, entity);
        entity.synchronizeReservations(mapper.toReservationEntities(aggregate.reservations(), now), now);
      }
      return entity;
    }).toList();
    stockItems.saveAll(entities);
  }
}

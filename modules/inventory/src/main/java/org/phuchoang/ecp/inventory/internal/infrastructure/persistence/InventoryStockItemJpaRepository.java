package org.phuchoang.ecp.inventory.internal.infrastructure.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JPA access for complete StockItem aggregates, never a query-side projection. */
interface InventoryStockItemJpaRepository extends JpaRepository<InventoryStockItemEntity, UUID> {

    @EntityGraph(attributePaths = "reservations")
    Optional<InventoryStockItemEntity> findAggregateById(UUID id);

    @EntityGraph(attributePaths = "reservations")
    List<InventoryStockItemEntity> findAllByIdIn(Collection<UUID> ids);

    @Query("select distinct stockItem from InventoryStockReservationEntity reservation "
        + "join reservation.stockItem stockItem left join fetch stockItem.reservations "
        + "where reservation.orderId = :orderId")
    List<InventoryStockItemEntity> findAggregatesByOrderId(@Param("orderId") UUID orderId);
}

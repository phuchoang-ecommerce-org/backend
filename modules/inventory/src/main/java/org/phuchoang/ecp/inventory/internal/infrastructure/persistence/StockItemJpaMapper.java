package org.phuchoang.ecp.inventory.internal.infrastructure.persistence;

import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.inventory.internal.domain.model.StockAdjustment;
import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;
import org.phuchoang.ecp.inventory.internal.domain.model.StockReservation;

import java.time.Instant;
import java.util.List;

/** Maps the framework-free StockItem write aggregate to its JPA-only representation. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface StockItemJpaMapper {

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    InventoryStockItemEntity toEntity(StockItem stockItem, @Context Instant now);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sku", ignore = true)
    @Mapping(target = "warehouseId", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "reservations", ignore = true)
    void updateEntity(StockItem stockItem, @MappingTarget InventoryStockItemEntity entity);

    StockItem toDomain(InventoryStockItemEntity entity);

    @Mapping(target = "stockItem", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    InventoryStockReservationEntity toEntity(StockReservation reservation, @Context Instant now);

    List<InventoryStockReservationEntity> toReservationEntities(List<StockReservation> reservations, @Context Instant now);

    @Mapping(target = "commit", ignore = true)
    StockReservation toDomain(InventoryStockReservationEntity entity);

    InventoryStockAdjustmentEntity toEntity(StockAdjustment adjustment);

    @AfterMapping
    default void initializeStockItem(StockItem stockItem, @MappingTarget InventoryStockItemEntity entity,
                                     @Context Instant now) {
        entity.initializeAuditTimestamps(now);
        entity.attachReservations();
    }

    @AfterMapping
    default void initializeReservation(StockReservation reservation, @MappingTarget InventoryStockReservationEntity entity,
                                       @Context Instant now) {
        entity.initializeAuditTimestamps(reservation.resolvedAt() == null ? now : reservation.resolvedAt(), now);
    }
}

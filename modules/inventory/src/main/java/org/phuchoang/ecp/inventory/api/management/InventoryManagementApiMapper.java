package org.phuchoang.ecp.inventory.api.management;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.inventory.internal.application.management.command.adjust.AdjustStockCommand;
import org.phuchoang.ecp.inventory.internal.application.management.command.adjust.AdjustStockResult;

import java.util.UUID;

/** Maps Inventory's management API contract to its application use-case contracts. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface InventoryManagementApiMapper {

    @Mapping(target = "caller", source = "caller")
    @Mapping(target = "correlationId", source = "correlationId")
    @Mapping(target = "stockItemId", source = "stockItemId")
    AdjustStockCommand adjustStockCommand(StockAdjustmentWrite adjustment, IdentityActor caller, UUID correlationId,
                                          UUID stockItemId);

    @Mapping(target = "reorderThreshold", ignore = true)
    StockItemView stockItemView(AdjustStockResult result);

    StockItemView stockItemView(org.phuchoang.ecp.inventory.internal.application.management.query.StockItemView view);

    StockAdjustmentView stockAdjustmentView(
        org.phuchoang.ecp.inventory.internal.application.management.query.StockAdjustmentView view);

    WarehouseView warehouseView(org.phuchoang.ecp.inventory.internal.application.management.query.WarehouseView view);
}

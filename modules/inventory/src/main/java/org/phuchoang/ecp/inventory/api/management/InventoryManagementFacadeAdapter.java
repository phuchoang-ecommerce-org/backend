package org.phuchoang.ecp.inventory.api.management;

import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.inventory.internal.application.management.command.adjust.AdjustStockService;
import org.phuchoang.ecp.inventory.internal.application.management.query.InventoryManagementQueryService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/** Public adapter delegating separately owned command and query use cases. */
@Component
class InventoryManagementFacadeAdapter implements InventoryManagementFacade {
    private final AdjustStockService commands;
    private final InventoryManagementQueryService queries;
    private final InventoryManagementApiMapper mapper;
    InventoryManagementFacadeAdapter(AdjustStockService commands, InventoryManagementQueryService queries,
                                     InventoryManagementApiMapper mapper) {
        this.commands = commands; this.queries = queries; this.mapper = mapper;
    }
    @Override public StockItemView adjustStock(IdentityActor caller, UUID correlationId, UUID stockItemId, StockAdjustmentWrite adjustment) {
        try {
            return mapper.stockItemView(commands.adjust(mapper.adjustStockCommand(adjustment, caller, correlationId, stockItemId)));
        } catch (org.phuchoang.ecp.inventory.internal.application.management.command.adjust.StockAdjustmentDeclinedException exception) {
            throw new StockAdjustmentDeclinedException(exception.reservedQuantity(), exception.orderIds());
        }
    }
    @Override public List<StockItemView> listStockItems(IdentityActor caller, String sku, UUID warehouseId, boolean lowStock, int limit) {
        return queries.list(caller, sku, warehouseId, lowStock, limit).stream().map(mapper::stockItemView).toList();
    }
    @Override public StockItemView getStockItem(IdentityActor caller, UUID stockItemId) { return mapper.stockItemView(queries.get(caller, stockItemId)); }
    @Override public List<StockAdjustmentView> listStockAdjustments(IdentityActor caller, UUID stockItemId, int limit) {
        return queries.adjustments(caller, stockItemId, limit).stream().map(mapper::stockAdjustmentView).toList();
    }
    @Override public List<WarehouseView> listWarehouses(IdentityActor caller, int limit) {
        return queries.warehouses(caller, limit).stream().map(mapper::warehouseView).toList();
    }
}

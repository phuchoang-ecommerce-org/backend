package org.phuchoang.ecp.inventory.api.management;

import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.inventory.internal.application.management.command.adjust.AdjustStockCommand;
import org.phuchoang.ecp.inventory.internal.application.management.command.adjust.AdjustStockResult;
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
    InventoryManagementFacadeAdapter(AdjustStockService commands, InventoryManagementQueryService queries) {
        this.commands = commands; this.queries = queries;
    }
    @Override public StockItemView adjustStock(IdentityActor caller, UUID correlationId, UUID stockItemId, StockAdjustmentWrite adjustment) {
        try {
            return toApiView(commands.adjust(new AdjustStockCommand(caller, correlationId, stockItemId, adjustment.delta(),
                adjustment.reasonCode(), adjustment.reason())));
        } catch (org.phuchoang.ecp.inventory.internal.application.management.command.adjust.StockAdjustmentDeclinedException exception) {
            throw new StockAdjustmentDeclinedException(exception.reservedQuantity(), exception.orderIds());
        }
    }
    @Override public List<StockItemView> listStockItems(IdentityActor caller, String sku, UUID warehouseId, boolean lowStock, int limit) {
        return queries.list(caller, sku, warehouseId, lowStock, limit).stream()
            .map(InventoryManagementFacadeAdapter::toApiView).toList();
    }
    @Override public StockItemView getStockItem(IdentityActor caller, UUID stockItemId) { return toApiView(queries.get(caller, stockItemId)); }
    @Override public List<StockAdjustmentView> listStockAdjustments(IdentityActor caller, UUID stockItemId, int limit) {
        return queries.adjustments(caller, stockItemId, limit).stream().map(InventoryManagementFacadeAdapter::toApiView).toList();
    }
    @Override public List<WarehouseView> listWarehouses(IdentityActor caller, int limit) {
        return queries.warehouses(caller, limit).stream().map(InventoryManagementFacadeAdapter::toApiView).toList();
    }

    private static StockItemView toApiView(AdjustStockResult result) {
        return new StockItemView(result.id(), result.sku(), result.warehouseId(), result.quantityOnHand(),
            result.quantityReserved(), result.availableQuantity(), null);
    }

    private static StockItemView toApiView(org.phuchoang.ecp.inventory.internal.application.management.query.StockItemView view) {
        return new StockItemView(view.id(), view.sku(), view.warehouseId(), view.quantityOnHand(), view.quantityReserved(),
            view.availableQuantity(), view.reorderThreshold());
    }

    private static StockAdjustmentView toApiView(org.phuchoang.ecp.inventory.internal.application.management.query.StockAdjustmentView view) {
        return new StockAdjustmentView(view.id(), view.stockItemId(), view.delta(), view.reasonCode(), view.reason(), view.actorId(),
            view.occurredAt());
    }

    private static WarehouseView toApiView(org.phuchoang.ecp.inventory.internal.application.management.query.WarehouseView view) {
        return new WarehouseView(view.id(), view.code(), view.name(), view.countryCode(), view.active());
    }
}

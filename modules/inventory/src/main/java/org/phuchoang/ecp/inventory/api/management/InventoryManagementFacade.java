package org.phuchoang.ecp.inventory.api.management;

import org.phuchoang.ecp.identity.api.authorization.IdentityActor;

import java.util.List;
import java.util.UUID;

/** Inventory's public operational command/query boundary. */
public interface InventoryManagementFacade {
    StockItemView adjustStock(IdentityActor caller, UUID correlationId, UUID stockItemId, StockAdjustmentWrite adjustment);
    List<StockItemView> listStockItems(IdentityActor caller, String sku, UUID warehouseId, boolean lowStock, int limit);
    StockItemView getStockItem(IdentityActor caller, UUID stockItemId);
    List<StockAdjustmentView> listStockAdjustments(IdentityActor caller, UUID stockItemId, int limit);
    List<WarehouseView> listWarehouses(IdentityActor caller, int limit);
}

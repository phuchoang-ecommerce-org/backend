package org.phuchoang.ecp.inventory.internal.application.management.query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read-side port; it intentionally returns operational views without reconstructing aggregates. */
public interface InventoryManagementQueries {
    List<StockItemView> listStockItems(String sku, UUID warehouseId, boolean lowStock, int limit);
    Optional<StockItemView> getStockItem(UUID stockItemId);
    List<StockAdjustmentView> listStockAdjustments(UUID stockItemId, int limit);
    List<WarehouseView> listWarehouses(int limit);
}

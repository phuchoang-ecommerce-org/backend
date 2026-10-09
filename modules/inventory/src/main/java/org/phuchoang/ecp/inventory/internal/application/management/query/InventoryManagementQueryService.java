package org.phuchoang.ecp.inventory.internal.application.management.query;

import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** UC-INV-05/US-ADM-05 query orchestration; no aggregate is loaded for an operational read. */
@Service
public class InventoryManagementQueryService {
    private static final String VIEW_INVENTORY = "viewInventory";
    private final IdentityAuthorization authorization;
    private final InventoryManagementQueries queries;

    public InventoryManagementQueryService(IdentityAuthorization authorization, InventoryManagementQueries queries) {
        this.authorization = authorization; this.queries = queries;
    }

    @Transactional(readOnly = true)
    public List<StockItemView> list(IdentityActor caller, String sku, UUID warehouseId, boolean lowStock, int limit) {
        authorization.assertAuthorized(caller, VIEW_INVENTORY);
        return queries.listStockItems(sku, warehouseId, lowStock, bounded(limit));
    }

    @Transactional(readOnly = true)
    public StockItemView get(IdentityActor caller, UUID stockItemId) {
        authorization.assertAuthorized(caller, VIEW_INVENTORY);
        return queries.getStockItem(stockItemId).orElseThrow(() -> new IllegalArgumentException("Unknown stock item."));
    }

    @Transactional(readOnly = true)
    public List<StockAdjustmentView> adjustments(IdentityActor caller, UUID stockItemId, int limit) {
        authorization.assertAuthorized(caller, VIEW_INVENTORY);
        return queries.listStockAdjustments(stockItemId, bounded(limit));
    }

    @Transactional(readOnly = true)
    public List<WarehouseView> warehouses(IdentityActor caller, int limit) {
        authorization.assertAuthorized(caller, VIEW_INVENTORY);
        return queries.listWarehouses(bounded(limit));
    }

    private static int bounded(int requested) { return requested <= 0 ? 20 : Math.min(requested, 100); }
}

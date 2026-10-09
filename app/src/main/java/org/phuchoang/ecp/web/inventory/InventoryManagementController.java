package org.phuchoang.ecp.web.inventory;

import org.phuchoang.ecp.inventory.api.management.InventoryManagementFacade;
import org.phuchoang.ecp.inventory.api.management.StockAdjustmentView;
import org.phuchoang.ecp.inventory.api.management.StockAdjustmentWrite;
import org.phuchoang.ecp.inventory.api.management.StockItemView;
import org.phuchoang.ecp.inventory.api.management.WarehouseView;
import org.phuchoang.ecp.web.common.pagination.Page;
import org.phuchoang.ecp.web.common.pagination.PageEnvelope;
import org.phuchoang.ecp.web.common.security.RequestContext;
import org.phuchoang.ecp.web.common.security.RequestContextResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** HTTP transport only for the protected operational inventory surface. */
@RestController
class InventoryManagementController {
    private final InventoryManagementFacade inventory;
    private final RequestContextResolver requestContext;
    InventoryManagementController(InventoryManagementFacade inventory, RequestContextResolver requestContext) {
        this.inventory = inventory; this.requestContext = requestContext;
    }

    @PatchMapping("/api/v1/inventory/stock-items/{id}/adjustments")
    StockItemView adjust(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @RequestBody StockAdjustmentWrite body) {
        RequestContext context = requestContext.resolve(jwt);
        return inventory.adjustStock(context.caller(), context.correlationId(), id, body);
    }

    @GetMapping("/api/v1/inventory/stock-items")
    PageEnvelope<StockItemView> list(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String sku,
            @RequestParam(required = false) UUID warehouseId, @RequestParam(defaultValue = "false") boolean lowStock,
            @RequestParam(defaultValue = "20") int limit) {
        RequestContext context = requestContext.resolve(jwt);
        List<StockItemView> items = inventory.listStockItems(context.caller(), sku, warehouseId, lowStock, limit);
        return new PageEnvelope<>(items, new Page(items.size(), null, null));
    }

    @GetMapping("/api/v1/inventory/stock-items/{id}")
    StockItemView get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return inventory.getStockItem(requestContext.resolve(jwt).caller(), id);
    }

    @GetMapping("/api/v1/inventory/stock-items/{id}/adjustments")
    PageEnvelope<StockAdjustmentView> adjustments(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @RequestParam(defaultValue = "20") int limit) {
        List<StockAdjustmentView> items = inventory.listStockAdjustments(requestContext.resolve(jwt).caller(), id, limit);
        return new PageEnvelope<>(items, new Page(items.size(), null, null));
    }

    @GetMapping("/api/v1/inventory/warehouses")
    PageEnvelope<WarehouseView> warehouses(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "20") int limit) {
        List<WarehouseView> items = inventory.listWarehouses(requestContext.resolve(jwt).caller(), limit);
        return new PageEnvelope<>(items, new Page(items.size(), null, null));
    }
}

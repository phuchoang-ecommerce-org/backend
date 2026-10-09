package org.phuchoang.ecp.inventory.internal.application.management.query;

import java.util.UUID;

/** Operational read model for a stock item. */
public record StockItemView(UUID id, String sku, UUID warehouseId, int quantityOnHand, int quantityReserved,
        int availableQuantity, Integer reorderThreshold) { }

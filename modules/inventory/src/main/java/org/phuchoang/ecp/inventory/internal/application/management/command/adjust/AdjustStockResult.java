package org.phuchoang.ecp.inventory.internal.application.management.command.adjust;

import java.util.UUID;

/** Command-owned result for the adjusted authoritative stock item. */
public record AdjustStockResult(UUID id, String sku, UUID warehouseId, int quantityOnHand, int quantityReserved,
        int availableQuantity) { }

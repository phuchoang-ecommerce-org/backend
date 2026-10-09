package org.phuchoang.ecp.inventory.api.management;

import java.util.UUID;

/** Operational stock figures; deliberately not part of a customer-facing catalog response. */
public record StockItemView(UUID id, String sku, UUID warehouseId, int quantityOnHand, int quantityReserved,
        int availableQuantity, Integer reorderThreshold) { }

package org.phuchoang.ecp.inventory.api.reservation;

import java.util.UUID;

/** A line that cannot be fully held, with its actual availability at evaluation time. */
public record StockShortfall(UUID orderLineId, String sku, int requestedQuantity, int availableQuantity) { }

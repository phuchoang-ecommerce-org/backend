package org.phuchoang.ecp.inventory.internal.application.reservation.command.reserve;

import java.util.UUID;

/** Application failure detail captured from the authoritative stock evaluation. */
public record StockShortfall(UUID orderLineId, String sku, int requestedQuantity, int availableQuantity) { }

package org.phuchoang.ecp.cart.api;

import java.time.Instant;
import java.util.UUID;

/** Live display of an unpriced stored line. */
public record CartLineView(UUID id, UUID variantId, String sku, String productName, String variantName, int quantity,
                           MoneyView unitPrice, MoneyView lineTotal, boolean stockShort, Integer availableQuantity,
                           boolean purchasable, String unpurchasableReason, Instant addedAt) { }

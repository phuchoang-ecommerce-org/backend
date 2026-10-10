package org.phuchoang.ecp.cart.internal.application.cart.query;

import java.time.Instant;
import java.util.UUID;

/** Application read model for a live cart line. */
public record CartLineView(UUID id, UUID variantId, String sku, String productName, String variantName, int quantity,
                           MoneyView unitPrice, MoneyView lineTotal, boolean stockShort, Integer availableQuantity,
                           boolean purchasable, String unpurchasableReason, Instant addedAt) { }

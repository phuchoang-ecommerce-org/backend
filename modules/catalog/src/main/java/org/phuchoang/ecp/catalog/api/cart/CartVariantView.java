package org.phuchoang.ecp.catalog.api.cart;

import org.phuchoang.ecp.catalog.api.view.common.MoneyView;

import java.util.UUID;

/** Current sellability information that Cart needs without seeing Catalog persistence. */
public record CartVariantView(UUID id, UUID productId, String sku, String productName, String variantName,
                              MoneyView currentPrice, boolean purchasable, Integer availableQuantity) {
}

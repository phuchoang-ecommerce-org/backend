package org.phuchoang.ecp.catalog.internal.application.cart.query;

import org.phuchoang.ecp.catalog.internal.application.productpricing.MoneyValue;

import java.util.Optional;
import java.util.UUID;

/** Query-owned model for Cart's live pricing and sellability lookup. */
public interface CartVariantQuery {
    Optional<CurrentVariant> find(UUID variantId);

    record CurrentVariant(UUID id, UUID productId, String sku, String productName, String variantName,
                          MoneyValue currentPrice, boolean purchasable, Integer availableQuantity) { }
}

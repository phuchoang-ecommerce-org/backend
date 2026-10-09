package org.phuchoang.ecp.cart.internal.application.cart;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/** Cart-owned port for Catalog's current facts. */
interface VariantGateway {
    Optional<Variant> find(UUID variantId);
    record Variant(UUID id, String sku, String productName, String variantName, BigDecimal price, String currency,
                   boolean purchasable, Integer availableQuantity) { }
}

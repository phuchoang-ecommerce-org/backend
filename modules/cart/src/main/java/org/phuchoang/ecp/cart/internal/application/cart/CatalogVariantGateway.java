package org.phuchoang.ecp.cart.internal.application.cart;

import org.phuchoang.ecp.catalog.api.cart.CatalogCartFacade;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** Maps Catalog's public contract to Cart's locally owned application port. */
@Component
class CatalogVariantGateway implements VariantGateway {
    private final CatalogCartFacade catalog;
    CatalogVariantGateway(CatalogCartFacade catalog) { this.catalog = catalog; }
    @Override public Optional<Variant> find(UUID variantId) {
        return catalog.findCurrentVariant(variantId).map(value -> new Variant(value.id(), value.sku(), value.productName(),
            value.variantName(), value.currentPrice().amount(), value.currentPrice().currency(), value.purchasable(),
            value.availableQuantity()));
    }
}

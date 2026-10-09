package org.phuchoang.ecp.catalog.api.cart;

import org.phuchoang.ecp.catalog.api.view.common.MoneyView;
import org.phuchoang.ecp.catalog.internal.application.cart.query.CartVariantQueryService;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** Public adapter for the Cart-specific Catalog query. */
@Component
class CatalogCartFacadeAdapter implements CatalogCartFacade {
    private final CartVariantQueryService variants;
    CatalogCartFacadeAdapter(CartVariantQueryService variants) { this.variants = variants; }
    @Override public Optional<CartVariantView> findCurrentVariant(UUID variantId) {
        return variants.find(variantId).map(value -> new CartVariantView(value.id(), value.productId(), value.sku(),
            value.productName(), value.variantName(), new MoneyView(value.currentPrice().amount(), value.currentPrice().currency()),
            value.purchasable(), value.availableQuantity()));
    }
}

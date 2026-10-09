package org.phuchoang.ecp.catalog.internal.application.cart.query;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/** Observational Catalog query for Cart; Cart never reads Catalog tables itself. */
@Service
public class CartVariantQueryService {
    private final CartVariantQuery variants;
    public CartVariantQueryService(CartVariantQuery variants) { this.variants = variants; }
    public Optional<CartVariantQuery.CurrentVariant> find(UUID variantId) { return variants.find(variantId); }
}

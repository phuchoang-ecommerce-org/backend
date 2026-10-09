package org.phuchoang.ecp.catalog.api.cart;

import java.util.Optional;
import java.util.UUID;

/** Cart's anti-corruption boundary to Catalog's current variant facts. */
public interface CatalogCartFacade {
    Optional<CartVariantView> findCurrentVariant(UUID variantId);
}

package org.phuchoang.ecp.catalog.internal.application.browse.variant;

import org.phuchoang.ecp.catalog.internal.application.browse.variant.VariantDetail;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Read port for a product's purchasable variants. */
public interface VariantBrowsePort {

    /** Variants whose options contain every {@code selected} pair (all variants for an empty selection). */
    List<VariantDetail> variants(UUID productId, Map<String, String> selected);

    Optional<VariantDetail> variant(UUID productId, UUID variantId);
}

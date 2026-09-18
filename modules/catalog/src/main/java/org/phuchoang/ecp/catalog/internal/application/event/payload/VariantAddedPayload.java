package org.phuchoang.ecp.catalog.internal.application.event.payload;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

/** `VariantAdded.v1`. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record VariantAddedPayload(UUID productId, List<UUID> variantIds, List<String> variantSkus,
        List<UUID> affectedCategoryIds, List<String> affectedCategorySlugs, UUID variantId, MoneyPayload listPrice) {
}

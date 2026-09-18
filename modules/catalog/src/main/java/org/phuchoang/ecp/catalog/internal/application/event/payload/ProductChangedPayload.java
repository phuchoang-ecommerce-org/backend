package org.phuchoang.ecp.catalog.internal.application.event.payload;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

/** `ProductCreated.v1`, `ProductUpdated.v1`, `ProductPublished.v1` — the whole product plus what it affects. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ProductChangedPayload(UUID productId, List<UUID> variantIds, List<String> variantSkus,
        List<UUID> affectedCategoryIds, List<String> affectedCategorySlugs, ProductPayload product) {
}

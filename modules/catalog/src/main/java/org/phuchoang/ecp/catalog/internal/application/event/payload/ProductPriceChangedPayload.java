package org.phuchoang.ecp.catalog.internal.application.event.payload;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

/** `ProductPriceChanged.v1`. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ProductPriceChangedPayload(UUID productId, List<UUID> variantIds, List<String> variantSkus,
        UUID variantId, MoneyPayload listPrice) {
}

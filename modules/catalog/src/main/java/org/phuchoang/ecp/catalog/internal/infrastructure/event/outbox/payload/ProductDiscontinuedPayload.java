package org.phuchoang.ecp.catalog.internal.infrastructure.event.outbox.payload;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

/** `ProductDiscontinued.v1`. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ProductDiscontinuedPayload(UUID productId, List<UUID> variantIds, List<String> variantSkus) {
}

package org.phuchoang.ecp.catalog.internal.application.event.payload;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

/** `CategoryChanged.v1` for a deleted category — only the identity and the listing it affected remain. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record CategoryRemovedPayload(UUID id, List<UUID> affectedCategoryIds, List<String> affectedCategorySlugs) {
}

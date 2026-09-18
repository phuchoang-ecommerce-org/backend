package org.phuchoang.ecp.catalog.internal.application.event.payload;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

/** `CategoryChanged.v1` for a created or edited category. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record CategoryChangedPayload(UUID id, UUID parentId, String name, String slug, String path, int depth,
        int sortOrder, List<UUID> affectedCategoryIds, List<String> affectedCategorySlugs) {
}

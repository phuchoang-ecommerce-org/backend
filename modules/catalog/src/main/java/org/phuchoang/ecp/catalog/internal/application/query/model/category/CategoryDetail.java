package org.phuchoang.ecp.catalog.internal.application.query.model.category;

import java.util.List;
import java.util.UUID;

/** One category and its ancestor chain. */
public record CategoryDetail(UUID id, UUID parentId, String name, String slug, int depth, int sortOrder,
        String imageUrl, boolean featured, List<CategoryRef> ancestors) {
}

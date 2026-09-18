package org.phuchoang.ecp.catalog.internal.application.query.model.category;

import java.util.List;
import java.util.UUID;

/** A category with its nested children, as the navigation tree renders it. */
public record CategoryNode(UUID id, UUID parentId, String name, String slug, int depth, int sortOrder,
        String imageUrl, boolean featured, List<CategoryRef> ancestors, List<CategoryNode> children) {
}

package org.phuchoang.ecp.catalog.internal.application.command.category;

import org.phuchoang.ecp.catalog.internal.domain.model.Category;

import java.util.UUID;

/** Application-owned command result for category administration. */
public record CategorySnapshot(UUID id, UUID parentId, String name, String slug, String path, int depth,
        String imageUrl, int sortOrder, boolean featured) {

    public static CategorySnapshot from(Category category) {
        return new CategorySnapshot(category.id(), category.parentId(), category.name(), category.slug(),
            category.path(), category.depth(), category.imageUrl(), category.sortOrder(), category.featured());
    }
}

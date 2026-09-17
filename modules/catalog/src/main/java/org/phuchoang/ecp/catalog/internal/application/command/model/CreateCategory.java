package org.phuchoang.ecp.catalog.internal.application.command.model;

import java.util.UUID;

/** Complete normalized input for creating a category and deriving its hierarchy path. */
public record CreateCategory(UUID id, UUID parentId, String name, String slug, String imageUrl, int sortOrder,
        boolean featured) { }

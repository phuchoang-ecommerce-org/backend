package org.phuchoang.ecp.catalog.internal.application.command.category;

import java.util.UUID;

/** Normalized input for creating a category; the service assigns the identifier and derives the slug. */
public record CreateCategory(UUID parentId, String name, String imageUrl, int sortOrder, boolean featured) {
}

package org.phuchoang.ecp.catalog.internal.application.command.category;

import java.util.UUID;

/** Normalized mutable-category values, including a requested (possibly new) parent. */
public record CategoryChange(String name, UUID parentId, String imageUrl, int sortOrder, boolean featured) {
}

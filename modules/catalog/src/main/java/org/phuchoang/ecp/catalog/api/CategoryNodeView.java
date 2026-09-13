package org.phuchoang.ecp.catalog.api;

import java.util.List;
import java.util.UUID;

/**
 * A category and its recursively nested child categories for navigation.
 *
 * @param id stable category identifier
 * @param parentId parent identifier, or {@code null} for a root category
 * @param name display name
 * @param slug human-readable category identifier, never a materialized path
 * @param depth zero-based depth within the category tree
 * @param sortOrder configured sibling display order
 * @param imageUrl optional category image URL
 * @param featured whether the category is featured
 * @param ancestors root-to-parent breadcrumb chain for subtree reads
 * @param children child categories in display order
 */
public record CategoryNodeView(
    /** Stable category identifier. */ UUID id,
    /** Parent identifier, or {@code null} for a root category. */ UUID parentId,
    /** Display name. */ String name,
    /** Human-readable category identifier, never a materialized path. */ String slug,
    /** Zero-based depth within the category tree. */ int depth,
    /** Configured sibling display order. */ int sortOrder,
    /** Optional category image URL. */ String imageUrl,
    /** Whether the category is designated for featured-category presentation. */ boolean featured,
    /** Root-to-parent chain when the node was loaded as part of a subtree. */ List<CategoryRefView> ancestors,
    /** Child categories in display order. */ List<CategoryNodeView> children) {
}

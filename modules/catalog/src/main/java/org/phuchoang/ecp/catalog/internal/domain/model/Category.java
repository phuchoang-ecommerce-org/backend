package org.phuchoang.ecp.catalog.internal.domain.model;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.util.UUID;

/**
 * The category aggregate's invariant used by Sprint 06 reads and Sprint 09 category moves:
 * no category may be its own ancestor (`BR-CAT-03`). The database enforces the direct case;
 * write-side moves will use this value-object check with their materialised paths.
 *
 * @param id stable category identifier
 * @param path slash-delimited materialized path, including leading and trailing slashes
 */
@AggregateRoot
public record Category(
    /** Stable category identifier. */ @Identity UUID id,
    UUID parentId,
    String name,
    String slug,
    /** Slash-delimited materialized path, including leading and trailing slashes. */ String path,
    int depth,
    String imageUrl,
    int sortOrder,
    boolean featured) {

    /**
     * Validates the minimum representation required to evaluate tree-move invariants.
     *
     * @throws IllegalArgumentException when the identifier or slash-delimited path is missing
     */
    public Category {
        if (id == null || path == null || !path.startsWith("/") || !path.endsWith("/")) {
            throw new IllegalArgumentException("Category requires a slash-delimited materialised path.");
        }
    }

    /** Compatibility constructor for the original minimal invariant representation. */
    public Category(UUID id, String path) {
        this(id, null, null, null, path, 0, null, 0, false);
    }

    public static Category create(UUID id, UUID parentId, String name, String slug, String imageUrl, int sortOrder,
            boolean featured, Category parent) {
        String path = parent == null ? "/" + id + "/" : parent.path + id + "/";
        int depth = parent == null ? 0 : parent.depth + 1;
        return new Category(id, parentId, name, slug, path, depth, imageUrl, sortOrder, featured);
    }

    /** Applies a category edit and derives its path from the requested parent. */
    public Category change(UUID updatedParentId, String updatedName, String updatedImageUrl, int updatedSortOrder,
            boolean updatedFeatured, Category parent) {
        if (!mayMoveBelow(parent)) {
            throw new CategoryHierarchyViolation("A category cannot be moved beneath itself or a descendant.");
        }
        String updatedPath = parent == null ? "/" + id + "/" : parent.path + id + "/";
        int updatedDepth = parent == null ? 0 : parent.depth + 1;
        return new Category(id, updatedParentId, updatedName, slug, updatedPath, updatedDepth, updatedImageUrl,
            updatedSortOrder, updatedFeatured);
    }

    /**
     * Determines whether this category can be moved beneath a proposed parent without creating a
     * cycle. A root move has no proposed parent and is therefore valid.
     *
     * @param proposedParent requested new parent, or {@code null} to make this category a root
     * @return {@code false} when the proposed parent is this category or one of its descendants
     */
    public boolean mayMoveBelow(Category proposedParent) {
        return proposedParent == null || !proposedParent.path.startsWith(path);
    }
}

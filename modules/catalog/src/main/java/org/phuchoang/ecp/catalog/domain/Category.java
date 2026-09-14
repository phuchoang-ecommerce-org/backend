package org.phuchoang.ecp.catalog.domain;

import java.util.UUID;

/**
 * The category aggregate's invariant used by Sprint 06 reads and Sprint 09 category moves:
 * no category may be its own ancestor (`BR-CAT-03`). The database enforces the direct case;
 * write-side moves will use this value-object check with their materialised paths.
 *
 * @param id stable category identifier
 * @param path slash-delimited materialized path, including leading and trailing slashes
 */
public record Category(
    /** Stable category identifier. */ UUID id,
    /** Slash-delimited materialized path, including leading and trailing slashes. */ String path) {

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

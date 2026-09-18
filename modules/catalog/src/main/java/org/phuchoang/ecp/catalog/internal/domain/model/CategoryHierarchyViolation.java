package org.phuchoang.ecp.catalog.internal.domain.model;

/** Raised by {@link Category} when a move would make a category its own ancestor (`BR-CAT-03`). */
public class CategoryHierarchyViolation extends RuntimeException {

    public CategoryHierarchyViolation(String message) {
        super(message);
    }
}

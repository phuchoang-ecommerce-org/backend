package org.phuchoang.ecp.catalog.internal.domain.model;

/**
 * A product's visibility state (`BR-CAT-02`); mirrors {@code ck_catalog_product_publication_status}.
 * Only {@code PUBLISHED} products are reachable through guest-facing reads.
 */
public enum PublicationStatus {
    DRAFT, PUBLISHED, UNPUBLISHED, DISCONTINUED;

    /** @throws IllegalArgumentException for {@code null} or any value outside this enum */
    public static PublicationStatus from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("A publication status is required.");
        }
        return valueOf(value);
    }
}

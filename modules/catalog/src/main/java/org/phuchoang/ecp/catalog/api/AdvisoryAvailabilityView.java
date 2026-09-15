package org.phuchoang.ecp.catalog.api;

/**
 * Catalog's temporary, non-binding stock projection. A {@code null} value on a variant means the
 * projection is not known yet and is intentionally omitted from JSON.
 *
 * @param inStock whether the variant was projected as purchasable
 */
public record AdvisoryAvailabilityView(boolean inStock) {
}

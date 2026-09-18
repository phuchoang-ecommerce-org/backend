package org.phuchoang.ecp.catalog.internal.application.query.model.product;

import org.phuchoang.ecp.catalog.internal.application.query.model.common.MoneyValue;

import java.util.UUID;

/** One row of a category listing, with variant-derived price range, stock advisory and cover image. */
public record ProductSummary(UUID id, String name, String slug, String brand, String publicationStatus,
        String primaryImageUrl, MoneyValue priceFrom, MoneyValue priceTo, Double averageRating,
        int reviewCount, Boolean inStock) {
}

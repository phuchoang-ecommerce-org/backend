package org.phuchoang.ecp.catalog.internal.application.browse.query.categoryproduct;

import org.phuchoang.ecp.catalog.internal.application.productpricing.MoneyValue;

import java.util.UUID;

/** One row of a category listing, with variant-derived price range, stock advisory and cover image. */
public record ProductSummary(UUID id, String name, String slug, String brand, String publicationStatus,
        String primaryImageUrl, MoneyValue priceFrom, MoneyValue priceTo, Double averageRating,
        int reviewCount, Boolean inStock) {
}

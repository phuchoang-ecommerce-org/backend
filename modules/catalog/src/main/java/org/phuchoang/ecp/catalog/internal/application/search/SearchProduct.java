package org.phuchoang.ecp.catalog.internal.application.search;

import org.phuchoang.ecp.catalog.internal.application.query.model.common.MoneyValue;

import java.util.UUID;

/** Projection-shaped product returned by Elasticsearch; it never becomes Catalog authority. */
public record SearchProduct(UUID id, String name, String slug, String brand, String primaryImageUrl,
        MoneyValue priceFrom, MoneyValue priceTo, Double averageRating, int reviewCount, Boolean inStock) {
}

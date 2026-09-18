package org.phuchoang.ecp.catalog.internal.infrastructure.persistence.browse.listing;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** One row of the {@code product_rows} CTE — the relational projection every listing page is cut from. */
record ProductListingRow(UUID id, String name, String slug, String brand, String status, String imageUrl,
        BigDecimal priceFrom, BigDecimal priceTo, String currency, Double averageRating, int reviewCount,
        Boolean inStock, OffsetDateTime createdAt) {
}

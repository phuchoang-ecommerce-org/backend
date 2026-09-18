package org.phuchoang.ecp.catalog.internal.application.query.model.listing;

import java.math.BigDecimal;
import java.util.List;

/** Normalized category-listing inputs: opaque cursor, page size, sort key, and filters. */
public record ProductListingQuery(String cursor, int size, String sort, List<String> brands,
        BigDecimal priceFrom, BigDecimal priceTo, Boolean inStock) {
}

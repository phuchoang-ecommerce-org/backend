package org.phuchoang.ecp.catalog.api.query;

import java.math.BigDecimal;
import java.util.List;

/** Public, normalized inputs for a category listing query. */
public record CatalogListingQuery(String cursor, int size, String sort, List<String> brands,
        BigDecimal priceFrom, BigDecimal priceTo, Boolean inStock) { }

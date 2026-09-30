package org.phuchoang.ecp.catalog.internal.application.search;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Query-side criteria for the Elasticsearch product projection. */
public record SearchProductsQuery(String keyword, String cursor, int size, String sort, UUID categoryId,
        List<String> brands, Map<String, String> attributes, BigDecimal priceFrom, BigDecimal priceTo,
        Boolean inStock) {

    public SearchProductsQuery {
        brands = List.copyOf(brands);
        attributes = Map.copyOf(attributes);
    }
}

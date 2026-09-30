package org.phuchoang.ecp.catalog.api.search;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Normalized inputs for a keyword or filtered product search. */
public record SearchProductsQuery(String keyword, String cursor, int size, String sort, UUID categoryId,
        List<String> brands, Map<String, String> attributes, BigDecimal priceFrom, BigDecimal priceTo,
        Boolean inStock) {

    public SearchProductsQuery {
        keyword = keyword == null ? null : keyword.trim();
        brands = brands == null ? List.of() : List.copyOf(brands);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}

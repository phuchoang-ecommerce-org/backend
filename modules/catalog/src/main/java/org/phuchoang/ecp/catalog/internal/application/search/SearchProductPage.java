package org.phuchoang.ecp.catalog.internal.application.search;

import java.util.List;

/** Query result from the product-search read-store port. */
public record SearchProductPage(List<SearchProduct> items, String nextCursor, Long matchCount,
        List<SearchFacet> facets, List<ActiveSearchFilter> activeFilters) {

    public SearchProductPage {
        items = List.copyOf(items);
        facets = List.copyOf(facets);
        activeFilters = List.copyOf(activeFilters);
    }
}

package org.phuchoang.ecp.catalog.api.search.view;

import org.phuchoang.ecp.catalog.api.view.product.ProductSummaryView;

import java.util.List;

/**
 * Cursor page returned by the search projection. {@code matchCount} is intentionally approximate;
 * callers must not treat it as the exact total used by ordinary catalog listings.
 */
public record SearchResultPageView(List<ProductSummaryView> items, String nextCursor, Long matchCount,
        List<SearchFacetView> facets, List<ActiveSearchFilterView> activeFilters, boolean degraded) {

    public SearchResultPageView {
        items = List.copyOf(items);
        facets = List.copyOf(facets);
        activeFilters = List.copyOf(activeFilters);
    }
}

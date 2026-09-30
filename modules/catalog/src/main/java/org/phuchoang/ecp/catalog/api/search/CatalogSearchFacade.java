package org.phuchoang.ecp.catalog.api.search;

import org.phuchoang.ecp.catalog.api.search.view.SearchResultPageView;

/**
 * Catalog's public, eventually-consistent product-search query boundary.
 *
 * <p>The underlying Elasticsearch projection is deliberately not exposed: callers receive the
 * search capability rather than a datastore contract.
 */
public interface CatalogSearchFacade {

    /** Searches the catalog projection using the complete, normalized query context. */
    SearchResultPageView searchProducts(SearchProductsQuery query);
}

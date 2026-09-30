package org.phuchoang.ecp.catalog.api.search;

import org.phuchoang.ecp.catalog.api.search.view.SearchResultPageView;
import org.phuchoang.ecp.catalog.internal.application.search.SearchProductQueryService;
import org.springframework.stereotype.Component;

/** Maps Catalog's internal Elasticsearch query model to its named public search interface. */
@Component
class CatalogSearchFacadeAdapter implements CatalogSearchFacade {

    private final SearchProductQueryService searches;

    public CatalogSearchFacadeAdapter(SearchProductQueryService searches) {
        this.searches = searches;
    }

    @Override
    public SearchResultPageView searchProducts(SearchProductsQuery query) {
        return CatalogSearchMapper.page(searches.search(CatalogSearchMapper.query(query)));
    }
}

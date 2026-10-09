package org.phuchoang.ecp.catalog.api.search;

import org.phuchoang.ecp.catalog.api.search.view.SearchResultPageView;
import org.phuchoang.ecp.catalog.api.error.DomainException;
import org.phuchoang.ecp.catalog.api.error.GenErrorCode;
import org.phuchoang.ecp.catalog.internal.application.error.ApplicationException;
import org.phuchoang.ecp.catalog.internal.application.search.query.SearchProductQueryService;
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
        try {
            return CatalogSearchMapper.page(searches.search(CatalogSearchMapper.query(query)));
        } catch (ApplicationException exception) {
            throw new DomainException(GenErrorCode.valueOf(exception.errorCode().name()), exception.getMessage());
        }
    }
}

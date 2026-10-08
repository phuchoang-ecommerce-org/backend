package org.phuchoang.ecp.catalog.internal.application.search;

import org.phuchoang.ecp.catalog.internal.application.error.ApplicationErrorCode;
import org.phuchoang.ecp.catalog.internal.application.error.ApplicationException;
import org.springframework.stereotype.Service;

/**
 * Query orchestration for the Catalog-owned search projection. It intentionally has no fallback
 * to Catalog's PostgreSQL read tables: ES unavailability is a declared capability failure.
 */
@Service
public class SearchProductQueryService {

    private final SearchProductsPort products;

    public SearchProductQueryService(SearchProductsPort products) {
        this.products = products;
    }

    public SearchProductPage search(SearchProductsQuery query) {
        try {
            return products.search(query);
        } catch (SearchStoreUnavailableException exception) {
            throw new ApplicationException(ApplicationErrorCode.DEPENDENCY_UNAVAILABLE,
                "Search is temporarily unavailable.");
        }
    }
}

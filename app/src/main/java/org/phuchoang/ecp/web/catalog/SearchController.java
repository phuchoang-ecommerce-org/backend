package org.phuchoang.ecp.web.catalog;

import jakarta.servlet.http.HttpServletRequest;
import org.phuchoang.ecp.catalog.api.search.CatalogSearchFacade;
import org.phuchoang.ecp.catalog.api.search.view.SearchResultPageView;
import org.phuchoang.ecp.web.common.pagination.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Public search endpoint backed exclusively by Catalog's Elasticsearch projection. */
@RestController
class SearchController {

    private final CatalogSearchFacade search;
    private final SearchHttpRequestMapper requests;

    SearchController(CatalogSearchFacade search, SearchHttpRequestMapper requests) {
        this.search = search;
        this.requests = requests;
    }

    @GetMapping("/api/v1/search/products")
    SearchResponse searchProducts(HttpServletRequest request, @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(required = false) String cursor, @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort, @RequestParam(required = false) UUID categoryId,
            @RequestParam(name = "brand", required = false) List<String> brands,
            @RequestParam(name = "attribute", required = false) List<String> attributes,
            @RequestParam(required = false) BigDecimal priceFrom, @RequestParam(required = false) BigDecimal priceTo,
            @RequestParam(required = false) Boolean inStock) {
        SearchResultPageView page = search.searchProducts(requests.query(request, keyword, cursor, size, sort, categoryId,
            brands, attributes, priceFrom, priceTo, inStock));
        return new SearchResponse(page.items(), new Page(page.items().size(), page.nextCursor(), null), page.matchCount(),
            page.facets(), page.activeFilters(), page.degraded());
    }

    /** Exact wire shape of OpenAPI's SearchResultPage: a page envelope plus search additions. */
    record SearchResponse(List<?> items, Page page, Long matchCount, List<?> facets, List<?> activeFilters,
            boolean degraded) {
    }
}

package org.phuchoang.ecp.catalog.api.search;

import org.phuchoang.ecp.catalog.api.search.view.ActiveSearchFilterView;
import org.phuchoang.ecp.catalog.api.search.view.SearchFacetView;
import org.phuchoang.ecp.catalog.api.search.view.SearchResultPageView;
import org.phuchoang.ecp.catalog.api.view.common.MoneyView;
import org.phuchoang.ecp.catalog.api.view.product.ProductSummaryView;
import org.phuchoang.ecp.catalog.internal.application.search.SearchProduct;
import org.phuchoang.ecp.catalog.internal.application.search.SearchProductPage;

/** Translation between Catalog's public search contract and its internal query read model. */
final class CatalogSearchMapper {

    private CatalogSearchMapper() {
    }

    static org.phuchoang.ecp.catalog.internal.application.search.SearchProductsQuery query(SearchProductsQuery source) {
        return new org.phuchoang.ecp.catalog.internal.application.search.SearchProductsQuery(source.keyword(), source.cursor(),
            source.size(), source.sort(), source.categoryId(), source.brands(), source.attributes(), source.priceFrom(),
            source.priceTo(), source.inStock());
    }

    static SearchResultPageView page(SearchProductPage source) {
        return new SearchResultPageView(source.items().stream().map(CatalogSearchMapper::product).toList(),
            source.nextCursor(), source.matchCount(), source.facets().stream()
                .map(facet -> new SearchFacetView(facet.field(), facet.values().stream()
                    .map(value -> new SearchFacetView.Value(value.value(), value.count())).toList()))
                .toList(),
            source.activeFilters().stream().map(filter -> new ActiveSearchFilterView(filter.field(), filter.value())).toList(),
            false);
    }

    private static ProductSummaryView product(SearchProduct source) {
        return new ProductSummaryView(source.id(), source.name(), source.slug(), source.brand(), "PUBLISHED",
            source.primaryImageUrl(), money(source.priceFrom()), money(source.priceTo()), source.averageRating(),
            source.reviewCount(), source.inStock());
    }

    private static MoneyView money(org.phuchoang.ecp.catalog.internal.application.query.model.common.MoneyValue source) {
        return source == null ? null : new MoneyView(source.amount(), source.currency());
    }
}

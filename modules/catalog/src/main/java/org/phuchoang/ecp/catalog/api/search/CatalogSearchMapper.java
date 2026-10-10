package org.phuchoang.ecp.catalog.api.search;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.catalog.api.search.view.ActiveSearchFilterView;
import org.phuchoang.ecp.catalog.api.search.view.SearchFacetView;
import org.phuchoang.ecp.catalog.api.search.view.SearchResultPageView;
import org.phuchoang.ecp.catalog.api.view.common.MoneyView;
import org.phuchoang.ecp.catalog.api.view.product.ProductSummaryView;
import org.phuchoang.ecp.catalog.internal.application.productpricing.MoneyValue;
import org.phuchoang.ecp.catalog.internal.application.search.query.ActiveSearchFilter;
import org.phuchoang.ecp.catalog.internal.application.search.query.SearchFacet;
import org.phuchoang.ecp.catalog.internal.application.search.query.SearchProduct;
import org.phuchoang.ecp.catalog.internal.application.search.query.SearchProductPage;

/** Translation between Catalog's public search contract and its internal query read model. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface CatalogSearchMapper {

    org.phuchoang.ecp.catalog.internal.application.search.query.SearchProductsQuery query(SearchProductsQuery source);

    @Mapping(target = "degraded", constant = "false")
    SearchResultPageView page(SearchProductPage source);

    @Mapping(target = "publicationStatus", constant = "PUBLISHED")
    ProductSummaryView product(SearchProduct source);

    MoneyView money(MoneyValue source);

    SearchFacetView facet(SearchFacet source);

    SearchFacetView.Value facetValue(SearchFacet.Value source);

    ActiveSearchFilterView activeSearchFilter(ActiveSearchFilter source);
}

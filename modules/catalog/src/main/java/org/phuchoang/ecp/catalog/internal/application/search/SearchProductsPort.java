package org.phuchoang.ecp.catalog.internal.application.search;

/** Read port for the Elasticsearch-backed product search projection. */
public interface SearchProductsPort {

    SearchProductPage search(SearchProductsQuery query);
}

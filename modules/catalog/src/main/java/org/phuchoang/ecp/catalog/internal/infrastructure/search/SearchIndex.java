package org.phuchoang.ecp.catalog.internal.infrastructure.search;

/** Stable query alias and the current immutable mapping version for Catalog's product index. */
public final class SearchIndex {

    public static final String ALIAS = "ecp-products";
    public static final String VERSIONED_NAME = "ecp-products-v1";
    public static final String MAPPING_RESOURCE = "elasticsearch/ecp-products-v1.json";

    private SearchIndex() {
    }
}

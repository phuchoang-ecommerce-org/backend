package org.phuchoang.ecp.catalog.internal.application.search;


/** Output port for the non-authoritative product-search document. */
public interface SearchProductProjectionWriter {

    void write(SearchProjectionEvent event);
}

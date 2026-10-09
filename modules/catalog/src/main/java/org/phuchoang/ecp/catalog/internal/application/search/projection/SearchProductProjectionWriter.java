package org.phuchoang.ecp.catalog.internal.application.search.projection;


/** Output port for the non-authoritative product-search document. */
public interface SearchProductProjectionWriter {

    void write(SearchProjectionEvent event);
}

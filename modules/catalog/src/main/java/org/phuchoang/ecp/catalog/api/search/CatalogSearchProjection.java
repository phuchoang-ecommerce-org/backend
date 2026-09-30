package org.phuchoang.ecp.catalog.api.search;

/**
 * Catalog's event-consumer entry point for the search read model.  The composition root owns
 * Kafka transport; Catalog owns the derived Elasticsearch document and never reads its write
 * tables to repair it.
 */
public interface CatalogSearchProjection {

    void project(CatalogSearchProjectionEvent event);
}

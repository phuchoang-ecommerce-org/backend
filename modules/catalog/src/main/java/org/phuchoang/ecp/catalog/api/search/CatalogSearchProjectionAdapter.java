package org.phuchoang.ecp.catalog.api.search;

import org.phuchoang.ecp.catalog.internal.application.search.projection.SearchProductProjectionWriter;
import org.phuchoang.ecp.catalog.internal.application.search.projection.SearchProjectionEvent;
import org.springframework.stereotype.Component;

/** Adapts Catalog's public event-consumer contract to its internal projection application flow. */
@Component
class CatalogSearchProjectionAdapter implements CatalogSearchProjection {

    private final SearchProductProjectionWriter projection;

    CatalogSearchProjectionAdapter(SearchProductProjectionWriter projection) {
        this.projection = projection;
    }

    @Override
    public void project(CatalogSearchProjectionEvent event) {
        projection.write(new SearchProjectionEvent(event.eventId(), event.eventType(), event.occurredAt(),
            event.aggregateType(), event.aggregateId(), event.correlationId(), event.payload()));
    }
}

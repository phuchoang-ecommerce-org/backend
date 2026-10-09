package org.phuchoang.ecp.catalog.internal.infrastructure.search;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.phuchoang.ecp.catalog.internal.application.search.projection.SearchProjectionLagProbe;
import org.springframework.stereotype.Component;

/** C2 projection freshness: now minus the newest applied Catalog envelope's occurredAt. */
@Component
class SearchProjectionMetrics {

    SearchProjectionMetrics(MeterRegistry registry, SearchProjectionLagProbe projection) {
        Gauge.builder("ecp.projection.lag.seconds", projection, SearchProjectionLagProbe::lagSeconds)
            .tag("projection", "product-search")
            .description("Lag of Catalog's Elasticsearch product-search projection")
            .register(registry);
    }
}

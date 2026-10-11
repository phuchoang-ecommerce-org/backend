package org.phuchoang.ecp.catalog.internal.application.event;

import org.phuchoang.ecp.catalog.internal.application.administration.command.CatalogCommandContext;
import org.phuchoang.ecp.catalog.internal.domain.model.CatalogEventSource;

/**
 * Application output port for publishing a Catalog business fact. The infrastructure adapter owns
 * the outbox, serialization, integration payload shape, and transport routing.
 */
public interface CatalogEventPublisher {

    /** Publishes facts drained from the aggregate root that raised them. */
    void publishFrom(CatalogEventSource aggregate, CatalogCommandContext context);
}

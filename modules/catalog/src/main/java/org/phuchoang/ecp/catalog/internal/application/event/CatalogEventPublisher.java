package org.phuchoang.ecp.catalog.internal.application.event;

import org.phuchoang.ecp.catalog.internal.application.administration.CatalogCommandContext;
import org.phuchoang.ecp.catalog.internal.domain.event.CatalogDomainEvent;

/**
 * Application output port for publishing a Catalog business fact. The infrastructure adapter owns
 * the outbox, serialization, integration payload shape, and transport routing.
 */
public interface CatalogEventPublisher {

    void publish(CatalogDomainEvent event, CatalogCommandContext context);
}

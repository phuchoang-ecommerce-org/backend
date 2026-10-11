package org.phuchoang.ecp.catalog.internal.domain.model;

import org.phuchoang.ecp.catalog.internal.domain.event.CatalogDomainEvent;

import java.util.List;

/** A Catalog aggregate root that owns domain facts raised by its state transitions. */
public sealed interface CatalogEventSource permits Product, Category {

    /** Returns and clears facts raised by this aggregate instance. */
    List<CatalogDomainEvent> pullDomainEvents();
}

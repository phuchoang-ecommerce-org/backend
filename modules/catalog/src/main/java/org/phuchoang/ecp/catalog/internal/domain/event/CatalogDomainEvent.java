package org.phuchoang.ecp.catalog.internal.domain.event;

import java.util.UUID;

/** A business fact emitted by the Catalog bounded context. */
public sealed interface CatalogDomainEvent permits ProductCreated, ProductUpdated, ProductPublished,
        ProductPriceChanged, ProductDiscontinued, VariantAdded, CategoryChanged {
    String eventType();
    String aggregateType();
    UUID aggregateId();
}

package org.phuchoang.ecp.catalog.internal.domain.event;

import org.jmolecules.event.annotation.DomainEvent;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import java.util.UUID;

/** Records creation of a draft Product aggregate for downstream catalog projections. */
@DomainEvent
public record ProductCreated(Product product) implements CatalogDomainEvent {
    @Override public String eventType() { return "ProductCreated"; }
    @Override public String aggregateType() { return "Product"; }
    @Override public UUID aggregateId() { return product.id(); }
}

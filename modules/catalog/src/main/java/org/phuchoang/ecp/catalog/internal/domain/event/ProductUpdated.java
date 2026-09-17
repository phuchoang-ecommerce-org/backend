package org.phuchoang.ecp.catalog.internal.domain.event;

import org.jmolecules.event.annotation.DomainEvent;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import java.util.UUID;

/** Records a product-content or merchandising update for catalog read-model refreshes. */
@DomainEvent
public record ProductUpdated(Product product) implements CatalogDomainEvent {
    @Override public String eventType() { return "ProductUpdated"; }
    @Override public String aggregateType() { return "Product"; }
    @Override public UUID aggregateId() { return product.id(); }
}

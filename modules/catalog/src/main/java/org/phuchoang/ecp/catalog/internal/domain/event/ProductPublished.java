package org.phuchoang.ecp.catalog.internal.domain.event;

import org.jmolecules.event.annotation.DomainEvent;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import java.util.UUID;

/** Records that a Product is available to the public catalog under {@code BR-CAT-02}. */
@DomainEvent
public record ProductPublished(Product product) implements CatalogDomainEvent {
    @Override public String eventType() { return "ProductPublished"; }
    @Override public String aggregateType() { return "Product"; }
    @Override public UUID aggregateId() { return product.id(); }
}

package org.phuchoang.ecp.catalog.internal.domain.event;

import org.jmolecules.event.annotation.DomainEvent;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import java.util.UUID;

/** Records a variant list-price change without changing prices frozen on existing orders. */
@DomainEvent
public record ProductPriceChanged(UUID productId, Product.Variant variant) implements CatalogDomainEvent {
    @Override public String eventType() { return "ProductPriceChanged"; }
    @Override public String aggregateType() { return "Product"; }
    @Override public UUID aggregateId() { return productId; }
}

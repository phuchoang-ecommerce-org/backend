package org.phuchoang.ecp.catalog.internal.domain.event;

import org.jmolecules.event.annotation.DomainEvent;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import java.util.UUID;

/** Records the addition of a uniquely identified purchasable variant to a Product. */
@DomainEvent
public record VariantAdded(UUID productId, Product.Variant variant) implements CatalogDomainEvent {
    @Override public String eventType() { return "VariantAdded"; }
    @Override public String aggregateType() { return "Product"; }
    @Override public UUID aggregateId() { return productId; }
}

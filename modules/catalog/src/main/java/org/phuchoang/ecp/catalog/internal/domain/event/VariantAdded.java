package org.phuchoang.ecp.catalog.internal.domain.event;

import org.jmolecules.event.annotation.DomainEvent;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import java.util.UUID;

/** Records the addition of a uniquely identified purchasable variant to a Product. */
@DomainEvent
/** {@code categoryId} names the listings the new variant affects. */
public record VariantAdded(UUID productId, UUID categoryId, Product product, Product.Variant variant) implements CatalogDomainEvent {

    /**
     * Compatibility constructor for older in-process callers. New command code supplies the
     * aggregate snapshot so an event-only projection can rebuild the complete product document.
     */
    public VariantAdded(UUID productId, UUID categoryId, Product.Variant variant) {
        this(productId, categoryId, null, variant);
    }

    public VariantAdded(Product product, Product.Variant variant) {
        this(product.id(), product.categoryId(), product, variant);
    }

    @Override public String eventType() { return "VariantAdded"; }
    @Override public String aggregateType() { return "Product"; }
    @Override public UUID aggregateId() { return productId; }
}

package org.phuchoang.ecp.catalog.internal.domain.event;

import org.jmolecules.event.annotation.DomainEvent;
import java.util.List;
import java.util.UUID;

/** Records withdrawal of a product and identifies the SKUs projections must remove from sale. */
@DomainEvent
public record ProductDiscontinued(UUID productId, List<UUID> variantIds, List<String> variantSkus)
        implements CatalogDomainEvent {
    public ProductDiscontinued {
        variantIds = List.copyOf(variantIds);
        variantSkus = List.copyOf(variantSkus);
    }
    @Override public String eventType() { return "ProductDiscontinued"; }
    @Override public String aggregateType() { return "Product"; }
    @Override public UUID aggregateId() { return productId; }
}

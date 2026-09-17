package org.phuchoang.ecp.catalog.internal.domain.event;

import org.jmolecules.event.annotation.DomainEvent;
import java.util.UUID;

/** Category facts carry their projection fields in the publisher because path expansion is persistence-owned. */
@DomainEvent
public record CategoryChanged(UUID categoryId) implements CatalogDomainEvent {
    @Override public String eventType() { return "CategoryChanged"; }
    @Override public String aggregateType() { return "Category"; }
    @Override public UUID aggregateId() { return categoryId; }
}

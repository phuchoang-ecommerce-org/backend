package org.phuchoang.ecp.catalog.internal.domain.event;

import org.jmolecules.event.annotation.DomainEvent;
import org.phuchoang.ecp.catalog.internal.domain.model.Category;

import java.util.UUID;

/** Category facts carry their projection fields in the publisher because path expansion is persistence-owned. */
@DomainEvent
public record CategoryChanged(Category category, boolean removed) implements CatalogDomainEvent {
    public static CategoryChanged of(Category category) { return new CategoryChanged(category, false); }
    public static CategoryChanged removed(Category category) { return new CategoryChanged(category, true); }
    @Override public String eventType() { return "CategoryChanged"; }
    @Override public String aggregateType() { return "Category"; }
    @Override public UUID aggregateId() { return category.id(); }
}

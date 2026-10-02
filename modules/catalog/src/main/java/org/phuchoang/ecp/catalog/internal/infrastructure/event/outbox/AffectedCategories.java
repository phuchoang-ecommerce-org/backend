package org.phuchoang.ecp.catalog.internal.infrastructure.event.outbox;

import org.phuchoang.ecp.catalog.internal.domain.model.SubtreeCategory;

import java.util.List;
import java.util.UUID;

/** The category listings a Catalog integration event invalidates: a subtree, as ids and slugs. */
record AffectedCategories(List<UUID> ids, List<String> slugs) {

    static final AffectedCategories NONE = new AffectedCategories(List.of(), List.of());

    static AffectedCategories of(List<SubtreeCategory> subtree) {
        return new AffectedCategories(subtree.stream().map(SubtreeCategory::id).toList(),
            subtree.stream().map(SubtreeCategory::slug).toList());
    }
}

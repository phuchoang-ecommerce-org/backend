package org.phuchoang.ecp.catalog.internal.application.event;

import org.phuchoang.ecp.catalog.internal.domain.model.SubtreeCategory;

import java.util.List;

/** The category listings a Catalog change invalidates: a subtree, as ids and as slugs. */
public record AffectedCategories(List<java.util.UUID> ids, List<String> slugs) {

    public static final AffectedCategories NONE = new AffectedCategories(List.of(), List.of());

    public static AffectedCategories of(List<SubtreeCategory> subtree) {
        return new AffectedCategories(subtree.stream().map(SubtreeCategory::id).toList(),
            subtree.stream().map(SubtreeCategory::slug).toList());
    }
}

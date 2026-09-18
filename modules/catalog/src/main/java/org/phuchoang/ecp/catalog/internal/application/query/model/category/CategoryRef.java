package org.phuchoang.ecp.catalog.internal.application.query.model.category;

import java.util.UUID;

/** A breadcrumb or membership reference to a category. */
public record CategoryRef(UUID id, String name, String slug) {
}

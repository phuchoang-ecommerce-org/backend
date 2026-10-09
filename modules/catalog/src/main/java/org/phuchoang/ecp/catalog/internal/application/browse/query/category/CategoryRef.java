package org.phuchoang.ecp.catalog.internal.application.browse.query.category;

import java.util.UUID;

/** A breadcrumb or membership reference to a category. */
public record CategoryRef(UUID id, String name, String slug) {
}

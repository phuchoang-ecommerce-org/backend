package org.phuchoang.ecp.catalog.internal.application.command.model;

import java.util.UUID;

/** Normalized mutable-category values applied by the Catalog application service. */
public record CategoryChange(String name, UUID parentId, String imageUrl, int sortOrder, boolean featured) { }

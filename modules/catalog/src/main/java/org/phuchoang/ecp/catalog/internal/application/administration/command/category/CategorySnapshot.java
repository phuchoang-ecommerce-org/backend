package org.phuchoang.ecp.catalog.internal.application.administration.command.category;

import java.util.UUID;

/** Application-owned command result for category administration. */
public record CategorySnapshot(UUID id, UUID parentId, String name, String slug, String path, int depth,
        String imageUrl, int sortOrder, boolean featured) {
}

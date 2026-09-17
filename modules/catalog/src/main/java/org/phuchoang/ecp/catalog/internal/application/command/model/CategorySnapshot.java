package org.phuchoang.ecp.catalog.internal.application.command.model;

import java.util.UUID;

/** Persistence-backed Category state used by administration commands. */
public record CategorySnapshot(UUID id, UUID parentId, String name, String slug, String path, int depth,
        String imageUrl, int sortOrder, boolean featured) {
    public boolean mayMoveBelow(CategorySnapshot proposedParent) {
        return proposedParent == null || !proposedParent.path().startsWith(path);
    }
}

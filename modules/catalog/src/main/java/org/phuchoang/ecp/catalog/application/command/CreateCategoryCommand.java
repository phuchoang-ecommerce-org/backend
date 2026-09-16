package org.phuchoang.ecp.catalog.application.command;

import org.phuchoang.ecp.sharedkernel.api.event.EventActor;

import java.util.UUID;

/** Internal write command used to exercise the catalog transactional-outbox boundary before Sprint 09 exposes HTTP. */
public record CreateCategoryCommand(UUID id, UUID parentId, String name, String slug, int sortOrder,
        UUID correlationId, EventActor actor) {

    public CreateCategoryCommand {
        if (id == null || name == null || name.isBlank() || slug == null || slug.isBlank() || correlationId == null) {
            throw new IllegalArgumentException("A category command requires id, name, slug, and correlation id.");
        }
    }
}

package org.phuchoang.ecp.catalog.internal.application.command;

import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.sharedkernel.api.event.EventActor;

import java.util.Objects;
import java.util.UUID;

/**
 * Who is executing a Catalog command and under which correlation id — the metadata every command
 * carries into authorization and into the events it appends to the outbox.
 */
public record CatalogCommandContext(IdentityActor caller, UUID correlationId) {

    public CatalogCommandContext {
        Objects.requireNonNull(caller, "caller");
        Objects.requireNonNull(correlationId, "correlationId");
    }

    /** The outbox attribution for this caller; {@code null} for an unauthenticated (guest) caller. */
    public EventActor actor() {
        if (caller.accountId() == null) {
            return null;
        }
        return new EventActor(caller.accountId(), caller.roles().stream().findFirst().orElse(null));
    }
}

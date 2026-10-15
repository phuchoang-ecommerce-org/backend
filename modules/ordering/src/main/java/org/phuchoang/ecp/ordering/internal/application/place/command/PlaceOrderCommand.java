package org.phuchoang.ecp.ordering.internal.application.place.command;

import org.phuchoang.ecp.identity.api.authorization.IdentityActor;

import java.util.Objects;
import java.util.UUID;

/** Application input for the idempotent place-order command. */
public record PlaceOrderCommand(IdentityActor caller, UUID checkoutId, String confirmationToken, String idempotencyKey,
        UUID correlationId) {
    public PlaceOrderCommand {
        Objects.requireNonNull(caller, "caller");
        Objects.requireNonNull(checkoutId, "checkoutId");
        Objects.requireNonNull(correlationId, "correlationId");
    }
}

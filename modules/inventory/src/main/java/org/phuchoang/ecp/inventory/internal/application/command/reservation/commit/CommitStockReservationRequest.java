package org.phuchoang.ecp.inventory.internal.application.command.reservation.commit;

import org.phuchoang.ecp.identity.api.authorization.IdentityActor;

import java.util.Objects;
import java.util.UUID;

/** Application input for committing one held stock reservation. */
public record CommitStockReservationRequest(IdentityActor caller, UUID stockItemId, UUID reservationId) {
    public CommitStockReservationRequest {
        Objects.requireNonNull(caller, "caller");
        Objects.requireNonNull(stockItemId, "stockItemId");
        Objects.requireNonNull(reservationId, "reservationId");
    }
}

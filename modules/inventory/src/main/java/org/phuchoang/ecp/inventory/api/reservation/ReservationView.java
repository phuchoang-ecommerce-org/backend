package org.phuchoang.ecp.inventory.api.reservation;

import java.time.Instant;
import java.util.UUID;

/** Stable representation of a held or resolved inventory reservation for the Ordering adapter. */
public record ReservationView(UUID reservationId, UUID stockItemId, UUID warehouseId, UUID orderId, UUID orderLineId, int quantity,
                              Status status, Instant expiresAt, Instant resolvedAt, Instant orphanedAt) {

    /** Compatibility constructor for integration callers that do not yet need the allocation location. */
    public ReservationView(UUID reservationId, UUID stockItemId, UUID orderId, UUID orderLineId, int quantity, Status status,
            Instant expiresAt, Instant resolvedAt, Instant orphanedAt) {
        this(reservationId, stockItemId, null, orderId, orderLineId, quantity, status, expiresAt, resolvedAt, orphanedAt);
    }

    public enum Status { HELD, COMMITTED, RELEASED }
}

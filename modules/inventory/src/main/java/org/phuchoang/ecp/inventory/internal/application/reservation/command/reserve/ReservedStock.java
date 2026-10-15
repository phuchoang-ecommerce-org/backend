package org.phuchoang.ecp.inventory.internal.application.reservation.command.reserve;

import java.time.Instant;
import java.util.UUID;

/** Application representation of a reservation returned by the reserve-stock use case. */
public record ReservedStock(
    UUID reservationId,
    UUID stockItemId,
    UUID warehouseId,
    UUID orderId,
    UUID orderLineId,
    int quantity,
    Status status,
    Instant expiresAt,
    Instant resolvedAt,
    Instant orphanedAt) {

    /** Compatibility constructor for callers compiled before warehouse allocation became part of placement. */
    public ReservedStock(UUID reservationId, UUID stockItemId, UUID orderId, UUID orderLineId, int quantity, Status status,
            Instant expiresAt, Instant resolvedAt, Instant orphanedAt) {
        this(reservationId, stockItemId, null, orderId, orderLineId, quantity, status, expiresAt, resolvedAt, orphanedAt);
    }

    public enum Status { HELD, COMMITTED, RELEASED }
}

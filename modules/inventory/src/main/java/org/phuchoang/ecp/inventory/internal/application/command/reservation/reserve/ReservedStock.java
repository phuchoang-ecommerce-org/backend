package org.phuchoang.ecp.inventory.internal.application.command.reservation.reserve;

import java.time.Instant;
import java.util.UUID;

/** Application representation of a reservation returned by the reserve-stock use case. */
public record ReservedStock(
    UUID reservationId,
    UUID stockItemId,
    UUID orderId,
    UUID orderLineId,
    int quantity,
    Status status,
    Instant expiresAt,
    Instant resolvedAt,
    Instant orphanedAt) {

    public enum Status { HELD, COMMITTED, RELEASED }
}

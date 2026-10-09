package org.phuchoang.ecp.inventory.internal.application.reservation.command.commit;

import java.time.Instant;
import java.util.UUID;

/** Application result of the authorised, idempotent UC-INV-03 commitment transition. */
public record CommitStockReservationResult(
    UUID reservationId,
    UUID stockItemId,
    UUID orderId,
    UUID orderLineId,
    int quantity,
    Status status,
    Instant expiresAt,
    Instant resolvedAt,
    Instant orphanedAt,
    Outcome outcome) {

    public enum Status { HELD, COMMITTED, RELEASED }

    public enum Outcome { COMMITTED, ALREADY_COMMITTED, DECLINED_ALREADY_RELEASED }
}

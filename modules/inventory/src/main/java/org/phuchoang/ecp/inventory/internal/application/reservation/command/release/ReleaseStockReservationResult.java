package org.phuchoang.ecp.inventory.internal.application.reservation.command.release;

import java.time.Instant;
import java.util.UUID;

/** Application result of the idempotent UC-INV-02 release transition. */
public record ReleaseStockReservationResult(
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

    public enum Outcome { RELEASED, ALREADY_RELEASED, DECLINED_ALREADY_COMMITTED }
}

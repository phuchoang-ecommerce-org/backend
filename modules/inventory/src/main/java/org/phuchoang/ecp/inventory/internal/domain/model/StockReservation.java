package org.phuchoang.ecp.inventory.internal.domain.model;

import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

import java.time.Instant;
import java.util.UUID;

/** A StockItem-owned promise of units to one allocated order line. */
@Entity
public record StockReservation(
    @Identity UUID id,
    UUID orderId,
    UUID orderLineId,
    int quantity,
    ReservationStatus status,
    Instant expiresAt,
    Instant resolvedAt,
    Instant orphanedAt) {

    public static StockReservation held(UUID id, UUID orderId, UUID orderLineId, int quantity, Instant expiresAt) {
        return new StockReservation(id, orderId, orderLineId, quantity, ReservationStatus.HELD, expiresAt, null, null);
    }

    public StockReservation release(Instant now, boolean orphaned) {
        return new StockReservation(id, orderId, orderLineId, quantity, ReservationStatus.RELEASED, expiresAt, now,
            orphaned ? now : orphanedAt);
    }

    public StockReservation commit(Instant now) {
        return new StockReservation(id, orderId, orderLineId, quantity, ReservationStatus.COMMITTED, expiresAt, now,
            orphanedAt);
    }
}

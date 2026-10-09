package org.phuchoang.ecp.inventory.internal.domain.model;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The single optimistic-locking consistency boundary for sellable stock (BR-INV-01).
 * {@code availableQuantity} is deliberately derived, never independently persisted.
 */
@AggregateRoot
public record StockItem(
    @Identity UUID id,
    String sku,
    UUID warehouseId,
    UUID ownerId,
    int quantityOnHand,
    int quantityReserved,
    long version,
    List<StockReservation> reservations) {

    public StockItem {
        if (quantityOnHand < 0 || quantityReserved < 0 || quantityReserved > quantityOnHand) {
            throw new IllegalArgumentException("Stock quantities must keep available stock non-negative.");
        }
        reservations = List.copyOf(reservations == null ? List.of() : reservations);
    }

    public static StockItem open(UUID id, String sku, UUID warehouseId, int quantityOnHand) {
        return new StockItem(id, sku, warehouseId, null, quantityOnHand, 0, 0, List.of());
    }

    public int availableQuantity() {
        return quantityOnHand - quantityReserved;
    }

    /**
     * Records a counted physical movement while preserving already-promised units (BR-INV-01).
     * The resulting child fact is persisted only through this aggregate root.
     */
    public Adjustment adjust(StockAdjustment proposedAdjustment, UUID adjustmentId, Instant occurredAt) {
        if (proposedAdjustment.recorded()) {
            throw new IllegalArgumentException("StockItem can only record a proposed stock adjustment.");
        }
        int adjustedOnHand = quantityOnHand + proposedAdjustment.delta();
        if (adjustedOnHand < quantityReserved) {
            throw new InsufficientAvailableStockException(availableQuantity());
        }
        StockItem adjusted = new StockItem(id, sku, warehouseId, ownerId, adjustedOnHand, quantityReserved, version,
            reservations);
        return new Adjustment(adjusted, proposedAdjustment.recordFor(adjustmentId, id, occurredAt));
    }

    public StockItem reserve(UUID reservationId, UUID orderId, UUID orderLineId, int quantity, Instant expiresAt) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Reservation quantity must be positive.");
        }
        if (availableQuantity() < quantity) {
            throw new InsufficientAvailableStockException(availableQuantity());
        }
        List<StockReservation> updated = new ArrayList<>(reservations);
        updated.add(StockReservation.held(reservationId, orderId, orderLineId, quantity, expiresAt));
        return new StockItem(id, sku, warehouseId, ownerId, quantityOnHand, quantityReserved + quantity, version, updated);
    }

    public Transition release(UUID reservationId, Instant now, boolean orphaned) {
        StockReservation reservation = requireReservation(reservationId);
        if (reservation.status() == ReservationStatus.RELEASED) {
            return new Transition(this, reservation, TransitionOutcome.ALREADY_RELEASED);
        }
        if (reservation.status() == ReservationStatus.COMMITTED) {
            return new Transition(this, reservation, TransitionOutcome.DECLINED_ALREADY_COMMITTED);
        }
        StockReservation released = reservation.release(now, orphaned);
        return new Transition(replace(reservationId, released, quantityOnHand, quantityReserved - reservation.quantity()),
            released, TransitionOutcome.RELEASED);
    }

    public Transition commit(UUID reservationId, Instant now) {
        StockReservation reservation = requireReservation(reservationId);
        if (reservation.status() == ReservationStatus.COMMITTED) {
            return new Transition(this, reservation, TransitionOutcome.ALREADY_COMMITTED);
        }
        if (reservation.status() == ReservationStatus.RELEASED) {
            return new Transition(this, reservation, TransitionOutcome.DECLINED_ALREADY_RELEASED);
        }
        StockReservation committed = reservation.commit(now);
        return new Transition(replace(reservationId, committed, quantityOnHand - reservation.quantity(),
            quantityReserved - reservation.quantity()), committed, TransitionOutcome.COMMITTED);
    }

    private StockReservation requireReservation(UUID reservationId) {
        return reservations.stream().filter(value -> value.id().equals(reservationId)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown stock reservation " + reservationId + "."));
    }

    private StockItem replace(UUID reservationId, StockReservation changed, int onHand, int reserved) {
        List<StockReservation> updated = reservations.stream().map(reservation -> reservation.id().equals(reservationId)
            ? changed : reservation).toList();
        return new StockItem(id, sku, warehouseId, ownerId, onHand, reserved, version, updated);
    }

    public record Transition(StockItem stockItem, StockReservation reservation, TransitionOutcome outcome) { }

    /** A root-owned state transition and its immutable movement child. */
    public record Adjustment(StockItem stockItem, StockAdjustment stockAdjustment) {
        public Adjustment {
            if (!stockItem.id().equals(stockAdjustment.stockItemId())) {
                throw new IllegalArgumentException("A stock adjustment must belong to its changed stock item.");
            }
            if (!stockAdjustment.recorded()) {
                throw new IllegalArgumentException("Only a recorded stock adjustment can be persisted with a stock item.");
            }
        }
    }

    public enum TransitionOutcome {
        RELEASED,
        ALREADY_RELEASED,
        COMMITTED,
        ALREADY_COMMITTED,
        DECLINED_ALREADY_COMMITTED,
        DECLINED_ALREADY_RELEASED
    }
}

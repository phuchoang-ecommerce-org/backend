package org.phuchoang.ecp.inventory.internal.domain.model;

import java.time.Instant;
import java.util.UUID;

import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

/**
 * Immutable, accountable record of a physical stock movement.
 *
 * <p>
 * The adjustment is part of Inventory's authoritative write model: it can
 * only be persisted as part of a successful {@link StockItem} adjustment.
 * </p>
 */
@Entity
public record StockAdjustment(
    @Identity UUID id,
    UUID stockItemId,
    int delta,
    String reasonCode,
    String reason,
    UUID actorId,
    Instant occurredAt) {

    public StockAdjustment {
        if (actorId == null) {
            throw new IllegalArgumentException("An inventory adjustment requires an actor.");
        }
        if (delta == 0 || reasonCode == null || reasonCode.isBlank() || reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Inventory adjustment requires a non-zero delta and reason.");
        }
        if (!allPresent(id, stockItemId, occurredAt) && !allAbsent(id, stockItemId, occurredAt)) {
            throw new IllegalArgumentException("An inventory adjustment is either proposed or fully recorded.");
        }
    }

    /** Creates validated adjustment intent before its owning aggregate records it. */
    public static StockAdjustment proposed(int delta, String reasonCode, String reason, UUID actorId) {
        return new StockAdjustment(null, null, delta, reasonCode, reason, actorId, null);
    }

    /** Records this movement for exactly one stock item; only recorded adjustments may be persisted. */
    public StockAdjustment recordFor(UUID adjustmentId, UUID stockItemId, Instant occurredAt) {
        if (recorded()) {
            throw new IllegalStateException("A stock adjustment is already recorded.");
        }
        return new StockAdjustment(adjustmentId, stockItemId, delta, reasonCode, reason, actorId, occurredAt);
    }

    public boolean recorded() {
        return id != null;
    }

    private static boolean allPresent(Object... values) {
        return java.util.Arrays.stream(values).allMatch(java.util.Objects::nonNull);
    }

    private static boolean allAbsent(Object... values) {
        return java.util.Arrays.stream(values).allMatch(java.util.Objects::isNull);
    }
}

package org.phuchoang.ecp.inventory.internal.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Persistence representation of an immutable inventory movement. */
@Entity
@Table(name = "inventory_stock_adjustment")
class InventoryStockAdjustmentEntity {
    @Id private UUID id;
    @Column(name = "stock_item_id", nullable = false) private UUID stockItemId;
    @Column(nullable = false) private int delta;
    @Column(name = "reason_code", nullable = false) private String reasonCode;
    @Column(nullable = false) private String reason;
    @Column(name = "actor_id", nullable = false) private UUID actorId;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;

    protected InventoryStockAdjustmentEntity() { }

    InventoryStockAdjustmentEntity(UUID id, UUID stockItemId, int delta, String reasonCode, String reason,
            UUID actorId, Instant occurredAt) {
        this.id = id; this.stockItemId = stockItemId; this.delta = delta; this.reasonCode = reasonCode;
        this.reason = reason; this.actorId = actorId; this.occurredAt = occurredAt;
    }
}

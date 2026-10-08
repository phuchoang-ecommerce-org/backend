package org.phuchoang.ecp.inventory.internal.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Persistence-only child of {@link InventoryStockItemEntity}. */
@Entity
@Table(name = "inventory_stock_reservation")
class InventoryStockReservationEntity {

    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_item_id", nullable = false)
    private InventoryStockItemEntity stockItem;
    @Column(name = "order_id", nullable = false)
    private UUID orderId;
    @Column(name = "order_line_id", nullable = false)
    private UUID orderLineId;
    @Column(nullable = false)
    private int quantity;
    @Column(nullable = false)
    private String status;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "resolved_at")
    private Instant resolvedAt;
    @Column(name = "orphaned_at")
    private Instant orphanedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InventoryStockReservationEntity() {
        // JPA
    }

    InventoryStockReservationEntity(UUID id, UUID orderId, UUID orderLineId, int quantity, String status,
            Instant expiresAt, Instant resolvedAt, Instant orphanedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.orderLineId = orderLineId;
        this.quantity = quantity;
        this.status = status;
        this.expiresAt = expiresAt;
        this.resolvedAt = resolvedAt;
        this.orphanedAt = orphanedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    void attachTo(InventoryStockItemEntity item) { stockItem = item; }
    void updateFrom(InventoryStockReservationEntity source) {
        status = source.status;
        resolvedAt = source.resolvedAt;
        orphanedAt = source.orphanedAt;
        updatedAt = source.updatedAt;
    }
    UUID id() { return id; }
    UUID orderId() { return orderId; }
    UUID orderLineId() { return orderLineId; }
    int quantity() { return quantity; }
    String status() { return status; }
    Instant expiresAt() { return expiresAt; }
    Instant resolvedAt() { return resolvedAt; }
    Instant orphanedAt() { return orphanedAt; }
}

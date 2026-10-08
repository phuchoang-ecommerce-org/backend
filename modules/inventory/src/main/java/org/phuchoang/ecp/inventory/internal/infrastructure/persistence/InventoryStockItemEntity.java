package org.phuchoang.ecp.inventory.internal.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** JPA-only representation of the StockItem aggregate root. */
@Entity
@Table(name = "inventory_stock_item")
class InventoryStockItemEntity {

    @Id
    private UUID id;
    @Column(nullable = false)
    private String sku;
    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;
    @Column(name = "owner_id")
    private UUID ownerId;
    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand;
    @Column(name = "quantity_reserved", nullable = false)
    private int quantityReserved;
    @Version
    private long version;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "stockItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InventoryStockReservationEntity> reservations = new ArrayList<>();

    protected InventoryStockItemEntity() {
        // JPA
    }

    InventoryStockItemEntity(UUID id, String sku, UUID warehouseId, UUID ownerId, int quantityOnHand,
            int quantityReserved, Instant now) {
        this.id = id;
        this.sku = sku;
        this.warehouseId = warehouseId;
        this.ownerId = ownerId;
        this.quantityOnHand = quantityOnHand;
        this.quantityReserved = quantityReserved;
        this.createdAt = now;
        this.updatedAt = now;
    }

    UUID id() { return id; }
    String sku() { return sku; }
    UUID warehouseId() { return warehouseId; }
    UUID ownerId() { return ownerId; }
    int quantityOnHand() { return quantityOnHand; }
    int quantityReserved() { return quantityReserved; }
    long version() { return version; }
    List<InventoryStockReservationEntity> reservations() { return reservations; }

    void synchronize(int onHand, int reserved, List<InventoryStockReservationEntity> replacements, Instant now) {
        quantityOnHand = onHand;
        quantityReserved = reserved;
        updatedAt = now;
        java.util.Map<UUID, InventoryStockReservationEntity> existing = reservations.stream()
            .collect(java.util.stream.Collectors.toMap(InventoryStockReservationEntity::id, value -> value));
        java.util.Set<UUID> retained = replacements.stream().map(InventoryStockReservationEntity::id)
            .collect(java.util.stream.Collectors.toSet());
        reservations.removeIf(reservation -> !retained.contains(reservation.id()));
        replacements.forEach(replacement -> {
            InventoryStockReservationEntity current = existing.get(replacement.id());
            if (current == null) {
                replacement.attachTo(this);
                reservations.add(replacement);
            } else {
                current.updateFrom(replacement);
            }
        });
    }
}

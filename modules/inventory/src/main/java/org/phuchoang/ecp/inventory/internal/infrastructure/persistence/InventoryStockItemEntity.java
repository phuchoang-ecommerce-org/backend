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

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getSku() {
    return sku;
  }

  public void setSku(String sku) {
    this.sku = sku;
  }

  public UUID getWarehouseId() {
    return warehouseId;
  }

  public void setWarehouseId(UUID warehouseId) {
    this.warehouseId = warehouseId;
  }

  public UUID getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(UUID ownerId) {
    this.ownerId = ownerId;
  }

  public int getQuantityOnHand() {
    return quantityOnHand;
  }

  public void setQuantityOnHand(int quantityOnHand) {
    this.quantityOnHand = quantityOnHand;
  }

  public int getQuantityReserved() {
    return quantityReserved;
  }

  public void setQuantityReserved(int quantityReserved) {
    this.quantityReserved = quantityReserved;
  }

  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
    this.version = version;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public List<InventoryStockReservationEntity> getReservations() {
    return reservations;
  }

  public void setReservations(List<InventoryStockReservationEntity> reservations) {
    this.reservations = reservations;
  }

  void initializeAuditTimestamps(Instant now) {
    createdAt = now;
    updatedAt = now;
  }

  void attachReservations() {
    reservations.forEach(reservation -> reservation.attachTo(this));
  }

  void synchronizeReservations(List<InventoryStockReservationEntity> replacements, Instant now) {
    updatedAt = now;
    java.util.Map<UUID, InventoryStockReservationEntity> existing = reservations.stream()
        .collect(java.util.stream.Collectors.toMap(InventoryStockReservationEntity::getId, value -> value));
    java.util.Set<UUID> retained = replacements.stream().map(InventoryStockReservationEntity::getId)
        .collect(java.util.stream.Collectors.toSet());
    reservations.removeIf(reservation -> !retained.contains(reservation.getId()));
    replacements.forEach(replacement -> {
      InventoryStockReservationEntity current = existing.get(replacement.getId());
      if (current == null) {
        replacement.attachTo(this);
        reservations.add(replacement);
      } else {
        current.updateFrom(replacement);
      }
    });
  }
}

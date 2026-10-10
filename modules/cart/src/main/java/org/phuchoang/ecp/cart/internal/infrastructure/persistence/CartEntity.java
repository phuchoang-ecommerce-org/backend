package org.phuchoang.ecp.cart.internal.infrastructure.persistence;

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

/** JPA representation only; the Cart domain record remains framework-free. */
@Entity
@Table(name = "cart_cart")
class CartEntity {
  @Id
  private UUID id;
  @Column(name = "customer_id")
  private UUID customerId;
  @Column(name = "session_token")
  private String guestToken;
  @Column(nullable = false)
  private String status;
  @Column(name = "last_activity_at", nullable = false)
  private Instant lastActivityAt;
  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;
  @Column(name = "merged_into_id")
  private UUID mergedIntoId;
  @Version
  private long version;
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
  @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<CartLineEntity> lines = new ArrayList<>();

  protected CartEntity() {
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public void setCustomerId(UUID customerId) {
    this.customerId = customerId;
  }

  public String getGuestToken() {
    return guestToken;
  }

  public void setGuestToken(String guestToken) {
    this.guestToken = guestToken;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instant getLastActivityAt() {
    return lastActivityAt;
  }

  public void setLastActivityAt(Instant lastActivityAt) {
    this.lastActivityAt = lastActivityAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public UUID getMergedIntoId() {
    return mergedIntoId;
  }

  public void setMergedIntoId(UUID mergedIntoId) {
    this.mergedIntoId = mergedIntoId;
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

  public List<CartLineEntity> getLines() {
    return lines;
  }

  public void setLines(List<CartLineEntity> lines) {
    this.lines = lines;
  }

  void initializeAuditTimestamps(Instant now) {
    createdAt = now;
    updatedAt = now;
  }

  void attachLines() {
    lines.forEach(line -> line.attachTo(this));
  }

  void synchronizeLines(List<CartLineEntity> replacements, Instant now) {
    updatedAt = now;
    java.util.Map<UUID, CartLineEntity> existing = lines.stream()
        .collect(java.util.stream.Collectors.toMap(CartLineEntity::getId, value -> value));
    java.util.Set<UUID> retained = replacements.stream().map(CartLineEntity::getId)
        .collect(java.util.stream.Collectors.toSet());
    lines.removeIf(line -> !retained.contains(line.getId()));
    for (CartLineEntity replacement : replacements) {
      CartLineEntity current = existing.get(replacement.getId());
      if (current == null) {
        replacement.attachTo(this);
        lines.add(replacement);
      } else
        current.synchronize(replacement);
    }
  }
}

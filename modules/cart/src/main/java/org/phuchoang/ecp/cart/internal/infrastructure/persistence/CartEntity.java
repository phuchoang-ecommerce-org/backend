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
    @Id private UUID id;
    @Column(name = "customer_id") private UUID customerId;
    @Column(name = "session_token") private String guestToken;
    @Column(nullable = false) private String status;
    @Column(name = "last_activity_at", nullable = false) private Instant lastActivityAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "merged_into_id") private UUID mergedIntoId;
    @Version private long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CartLineEntity> lines = new ArrayList<>();
    protected CartEntity() { }
    CartEntity(UUID id, UUID customerId, String guestToken, String status, Instant lastActivityAt, Instant expiresAt,
               UUID mergedIntoId, Instant now) {
        this.id = id; this.customerId = customerId; this.guestToken = guestToken; this.status = status;
        this.lastActivityAt = lastActivityAt; this.expiresAt = expiresAt; this.mergedIntoId = mergedIntoId;
        this.createdAt = now; this.updatedAt = now;
    }
    UUID id() { return id; } UUID customerId() { return customerId; } String guestToken() { return guestToken; }
    String status() { return status; } Instant lastActivityAt() { return lastActivityAt; } Instant expiresAt() { return expiresAt; }
    UUID mergedIntoId() { return mergedIntoId; } long version() { return version; } List<CartLineEntity> lines() { return lines; }
    void synchronize(String newStatus, Instant newLastActivityAt, Instant newExpiresAt, UUID newMergedIntoId,
                     List<CartLineEntity> replacements, Instant now) {
        status = newStatus; lastActivityAt = newLastActivityAt; expiresAt = newExpiresAt; mergedIntoId = newMergedIntoId; updatedAt = now;
        java.util.Map<UUID, CartLineEntity> existing = lines.stream().collect(java.util.stream.Collectors.toMap(CartLineEntity::id, value -> value));
        java.util.Set<UUID> retained = replacements.stream().map(CartLineEntity::id).collect(java.util.stream.Collectors.toSet());
        lines.removeIf(line -> !retained.contains(line.id()));
        for (CartLineEntity replacement : replacements) {
            CartLineEntity current = existing.get(replacement.id());
            if (current == null) { replacement.attachTo(this); lines.add(replacement); }
            else current.synchronize(replacement);
        }
    }
}

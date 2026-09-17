package org.phuchoang.ecp.catalog.internal.infrastructure.persistence.write.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Product-owned persistence child; it deliberately has no repository. */
@Entity
@Table(name = "catalog_product_image")
class CatalogProductImageEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "product_id", nullable = false) private CatalogProductEntity product;
    @Column(nullable = false) private String url;
    @Column(name = "alt_text") private String altText;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected CatalogProductImageEntity() { }
    CatalogProductImageEntity(UUID id, String url, String altText, int sortOrder, Instant now) {
        this.id = id; this.url = url; this.altText = altText; this.sortOrder = sortOrder; this.createdAt = now; this.updatedAt = now;
    }
    void attachTo(CatalogProductEntity product) { this.product = product; }
    UUID id() { return id; }
    String url() { return url; }
    String altText() { return altText; }
    int sortOrder() { return sortOrder; }
}

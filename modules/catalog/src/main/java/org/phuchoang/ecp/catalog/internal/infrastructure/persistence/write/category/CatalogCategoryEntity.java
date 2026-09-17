package org.phuchoang.ecp.catalog.internal.infrastructure.persistence.write.category;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/** Persistence-only shape of Catalog's Category aggregate. */
@Entity
@Table(name = "catalog_category")
class CatalogCategoryEntity {
    @Id private UUID id;
    @Column(name = "parent_id") private UUID parentId;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String slug;
    @Column(nullable = false) private String path;
    @Column(nullable = false) private int depth;
    @Column(name = "image_url") private String imageUrl;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(nullable = false) private boolean featured;
    @Version private long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected CatalogCategoryEntity() { }
    CatalogCategoryEntity(UUID id, UUID parentId, String name, String slug, String path, int depth, String imageUrl,
            int sortOrder, boolean featured, Instant now) {
        this.id = id; this.parentId = parentId; this.name = name; this.slug = slug; this.path = path; this.depth = depth;
        this.imageUrl = imageUrl; this.sortOrder = sortOrder; this.featured = featured; this.createdAt = now; this.updatedAt = now;
    }
    void update(UUID parentId, String name, String path, int depth, String imageUrl, int sortOrder, boolean featured, Instant now) {
        this.parentId = parentId; this.name = name; this.path = path; this.depth = depth; this.imageUrl = imageUrl;
        this.sortOrder = sortOrder; this.featured = featured; this.updatedAt = now;
    }
    UUID id() { return id; }
    UUID parentId() { return parentId; }
    String name() { return name; }
    String slug() { return slug; }
    String path() { return path; }
    int depth() { return depth; }
    String imageUrl() { return imageUrl; }
    int sortOrder() { return sortOrder; }
    boolean featured() { return featured; }
}

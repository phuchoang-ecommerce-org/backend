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
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    UUID parentId() { return parentId; }
    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }
    String name() { return name; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    String slug() { return slug; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    String path() { return path; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    int depth() { return depth; }
    public int getDepth() { return depth; }
    public void setDepth(int depth) { this.depth = depth; }
    String imageUrl() { return imageUrl; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    int sortOrder() { return sortOrder; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    boolean featured() { return featured; }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public void initializeAuditTimestamps(Instant now) { this.createdAt = now; this.updatedAt = now; }
}

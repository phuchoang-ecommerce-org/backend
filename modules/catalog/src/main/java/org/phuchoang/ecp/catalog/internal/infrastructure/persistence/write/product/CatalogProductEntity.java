package org.phuchoang.ecp.catalog.internal.infrastructure.persistence.write.product;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Persistence-only shape of Catalog's Product aggregate. */
@Entity
@Table(name = "catalog_product")
class CatalogProductEntity {

    @Id
    private UUID id;
    @Column(name = "category_id", nullable = false)
    private UUID categoryId;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String slug;
    private String description;
    private String brand;
    @Column(name = "publication_status", nullable = false)
    private String publicationStatus;
    @Column(name = "published_at")
    private Instant publishedAt;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String attributes;
    @Version
    private long version;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sku ASC")
    private List<CatalogVariantEntity> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC, id ASC")
    private List<CatalogProductImageEntity> images = new ArrayList<>();

    protected CatalogProductEntity() {
        // JPA
    }

    CatalogProductEntity(UUID id, UUID categoryId, String name, String slug, String description, String brand,
            String attributes, Instant now) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.brand = brand;
        this.attributes = attributes;
        this.publicationStatus = "DRAFT";
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Copies the aggregate's state verbatim — publication rules (first-publish stamping) belong to {@code Product}. */
    void update(String name, String description, String brand, UUID categoryId, String attributes,
            String publicationStatus, Instant publishedAt, Instant now) {
        this.name = name;
        this.description = description;
        this.brand = brand;
        this.categoryId = categoryId;
        this.attributes = attributes;
        this.publicationStatus = publicationStatus;
        this.publishedAt = publishedAt;
        this.updatedAt = now;
    }

    void addVariant(CatalogVariantEntity variant) {
        variant.attachTo(this);
        variants.add(variant);
    }

    void removeVariant(UUID variantId) {
        variants.removeIf(variant -> variant.id().equals(variantId));
    }

    void addImage(CatalogProductImageEntity image) {
        image.attachTo(this);
        images.add(image);
    }

    void removeImage(UUID imageId) {
        images.removeIf(image -> image.id().equals(imageId));
    }

    UUID id() { return id; }
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    UUID categoryId() { return categoryId; }
    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
    String name() { return name; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    String slug() { return slug; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    String description() { return description; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    String brand() { return brand; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    String publicationStatus() { return publicationStatus; }
    public String getPublicationStatus() { return publicationStatus; }
    public void setPublicationStatus(String publicationStatus) { this.publicationStatus = publicationStatus; }
    Instant publishedAt() { return publishedAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
    String attributes() { return attributes; }
    public String getAttributes() { return attributes; }
    public void setAttributes(String attributes) { this.attributes = attributes; }
    List<CatalogVariantEntity> variants() { return variants; }
    public List<CatalogVariantEntity> getVariants() { return variants; }
    List<CatalogProductImageEntity> images() { return images; }
    public List<CatalogProductImageEntity> getImages() { return images; }
    public void initializeAuditTimestamps(Instant now) { this.createdAt = now; this.updatedAt = now; }
    public void touch(Instant now) { this.updatedAt = now; }
}

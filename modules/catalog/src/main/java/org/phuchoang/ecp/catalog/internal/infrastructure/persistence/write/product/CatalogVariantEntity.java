package org.phuchoang.ecp.catalog.internal.infrastructure.persistence.write.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Product-owned persistence child; it deliberately has no repository. */
@Entity
@Table(name = "catalog_variant")
class CatalogVariantEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "product_id", nullable = false) private CatalogProductEntity product;
    @Column(nullable = false) private String sku;
    @Column(nullable = false) private String name;
    @Column(name = "list_price_amount", nullable = false) private BigDecimal amount;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "list_price_currency", nullable = false, length = 3) private String currency;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private String options;
    @Column(name = "weight_grams") private Integer weightGrams;
    @Column(name = "is_active", nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected CatalogVariantEntity() { }
    CatalogVariantEntity(UUID id, String sku, String name, BigDecimal amount, String currency, String options,
            Integer weightGrams, boolean active, Instant now) {
        this.id = id; this.sku = sku; this.name = name; this.amount = amount; this.currency = currency;
        this.options = options; this.weightGrams = weightGrams; this.active = active; this.createdAt = now; this.updatedAt = now;
    }
    void attachTo(CatalogProductEntity product) { this.product = product; }
    void changePrice(BigDecimal amount, String currency, Instant now) { this.amount = amount; this.currency = currency; this.updatedAt = now; }
    UUID id() { return id; }
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    String sku() { return sku; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    String name() { return name; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    BigDecimal amount() { return amount; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    String currency() { return currency; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    String options() { return options; }
    public String getOptions() { return options; }
    public void setOptions(String options) { this.options = options; }
    Integer weightGrams() { return weightGrams; }
    public Integer getWeightGrams() { return weightGrams; }
    public void setWeightGrams(Integer weightGrams) { this.weightGrams = weightGrams; }
    boolean active() { return active; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public void initializeAuditTimestamps(Instant now) { this.createdAt = now; this.updatedAt = now; }
}

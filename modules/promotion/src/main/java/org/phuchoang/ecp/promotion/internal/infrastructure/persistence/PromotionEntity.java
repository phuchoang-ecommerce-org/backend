package org.phuchoang.ecp.promotion.internal.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** JPA-only representation of the Promotion aggregate. */
@Entity
@Table(name = "promotion_promotion")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class PromotionEntity {
    @Id
    private UUID id;
    private String code;
    private String name;
    private String description;
    @Column(name = "discount_type")
    private String discountType;
    @Column(name = "discount_value")
    private BigDecimal discountValue;
    @Column(name = "max_discount_amount")
    private BigDecimal maxDiscountAmount;
    @Column(name = "min_order_amount")
    private BigDecimal minOrderAmount;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 3)
    private String currency;
    @Column(name = "valid_from")
    private Instant validFrom;
    @Column(name = "valid_until")
    private Instant validUntil;
    @Column(name = "usage_limit")
    private int usageLimit;
    @Column(name = "usage_count")
    private int usageCount;
    @Column(name = "per_customer_limit")
    private int perCustomerLimit;
    private int priority;
    private boolean stackable;
    private String status;
    @Column(name = "scheduled_status")
    private String scheduledStatus;
    @Column(name = "scheduled_at")
    private Instant scheduledAt;
    @Version
    private long version;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;
}

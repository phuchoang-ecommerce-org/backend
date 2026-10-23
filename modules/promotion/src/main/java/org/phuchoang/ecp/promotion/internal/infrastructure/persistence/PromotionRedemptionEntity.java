package org.phuchoang.ecp.promotion.internal.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Persistence evidence of a successful redemption, separate from the aggregate
 * root.
 */
@Entity
@Table(name = "promotion_redemption")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class PromotionRedemptionEntity {
    @Id
    private UUID id;
    @Column(name = "promotion_id")
    private UUID promotionId;
    @Column(name = "order_id")
    private UUID orderId;
    @Column(name = "customer_id")
    private UUID customerId;
    @Column(name = "discount_amount")
    private BigDecimal discountAmount;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 3)
    private String currency;
    @Column(name = "voucher_code")
    private String voucherCode;
    @Column(name = "redeemed_at")
    private Instant redeemedAt;
    @Column(name = "created_at")
    private Instant createdAt;
}

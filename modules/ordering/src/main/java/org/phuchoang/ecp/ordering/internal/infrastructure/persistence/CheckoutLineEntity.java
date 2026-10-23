package org.phuchoang.ecp.ordering.internal.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

/** Persistence child owned solely by {@link CheckoutEntity}. */
@Entity
@Table(name = "ordering_order_line")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class CheckoutLineEntity {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private CheckoutEntity checkout;
    @Column(name = "variant_id", nullable = false)
    private UUID variantId;
    @Column(nullable = false)
    private String sku;
    @Column(name = "product_name", nullable = false)
    private String productName;
    @Column(name = "variant_name")
    private String variantName;
    @Column(nullable = false)
    private int quantity;
    @Column(name = "unit_price_at_order_amount", nullable = false)
    private BigDecimal unitPriceAmount;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "unit_price_at_order_currency", nullable = false, length = 3)
    private String unitPriceCurrency;
    @Column(name = "line_discount_amount", nullable = false)
    private BigDecimal lineDiscountAmount;
    @Column(name = "line_total_amount", nullable = false)
    private BigDecimal lineTotalAmount;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

package org.phuchoang.ecp.ordering.internal.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA representation of the Draft order; checkout domain types remain
 * persistence-free.
 */
@Entity
@Table(name = "ordering_order")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class CheckoutEntity {
    @Id
    private UUID id;
    @Column(name = "order_number", nullable = false)
    private String orderNumber;
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
    @Column(nullable = false)
    private String status;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, columnDefinition = "char(3)")
    private String currency;
    @Column(name = "subtotal_amount", nullable = false)
    private BigDecimal subtotalAmount;
    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount;
    @Column(name = "shipping_fee_amount", nullable = false)
    private BigDecimal shippingFeeAmount;
    @Column(name = "tax_amount", nullable = false)
    private BigDecimal taxAmount;
    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;
    @Column(name = "promotion_id")
    private UUID promotionId;
    @Column(name = "promotion_code")
    private String promotionCode;
    @Column(name = "shipping_recipient_name")
    private String shippingRecipientName;
    @Column(name = "shipping_line1")
    private String shippingLine1;
    @Column(name = "shipping_line2")
    private String shippingLine2;
    @Column(name = "shipping_city")
    private String shippingCity;
    @Column(name = "shipping_region")
    private String shippingRegion;
    @Column(name = "shipping_postal_code")
    private String shippingPostalCode;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "shipping_country_code", columnDefinition = "char(2)")
    private String shippingCountryCode;
    @Column(name = "billing_recipient_name")
    private String billingRecipientName;
    @Column(name = "billing_line1")
    private String billingLine1;
    @Column(name = "billing_line2")
    private String billingLine2;
    @Column(name = "billing_city")
    private String billingCity;
    @Column(name = "billing_region")
    private String billingRegion;
    @Column(name = "billing_postal_code")
    private String billingPostalCode;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "billing_country_code", columnDefinition = "char(2)")
    private String billingCountryCode;
    @Column(name = "checkout_shipping_option_id")
    private String shippingOptionId;
    @Column(name = "checkout_shipping_carrier")
    private String shippingCarrier;
    @Column(name = "checkout_shipping_service")
    private String shippingService;
    @Column(name = "checkout_estimated_delivery_from")
    private Instant estimatedDeliveryFrom;
    @Column(name = "checkout_estimated_delivery_to")
    private Instant estimatedDeliveryTo;
    @Column(name = "checkout_confirmation_token")
    private String confirmationToken;
    @Version
    private long version;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @OneToMany(mappedBy = "checkout", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CheckoutLineEntity> lines = new ArrayList<>();

    void replaceLines(List<CheckoutLineEntity> replacements) {
        lines.clear();
        replacements.forEach(line -> {
            line.setCheckout(this);
            lines.add(line);
        });
    }
}

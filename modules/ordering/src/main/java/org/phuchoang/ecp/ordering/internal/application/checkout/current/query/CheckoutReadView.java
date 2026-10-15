package org.phuchoang.ecp.ordering.internal.application.checkout.current.query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Read model owned by the get-current-checkout use case. */
public record CheckoutReadView(UUID id, String orderNumber, UUID customerId, List<Line> lines, Money subtotal, Money discount,
        Money shippingFee, Money total, UUID promotionId, String promotionCode, Address shippingAddress, Address billingAddress,
        ShippingOption shippingOption, boolean ready) {
    public CheckoutReadView { lines = List.copyOf(lines); }
    public record Line(UUID id, UUID variantId, String sku, String productName, String variantName, int quantity, Money unitPrice,
            Money lineTotal) { }
    public record Money(BigDecimal amount, String currency) { }
    public record Address(String label, String recipientName, String line1, String line2, String city, String region, String postalCode,
            String countryCode, String phone) { }
    public record ShippingOption(String id, String carrier, String serviceName, Money fee, Instant estimatedDeliveryFrom,
            Instant estimatedDeliveryTo) { }
}

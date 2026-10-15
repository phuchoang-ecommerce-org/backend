package org.phuchoang.ecp.ordering.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Public checkout contract matching the shared OpenAPI Checkout shape. */
public record CheckoutView(OrderView order, List<ShippingOptionView> shippingOptions, String selectedShippingOptionId,
        boolean ready, List<Blocker> blockers) {
    public CheckoutView { shippingOptions = List.copyOf(shippingOptions); blockers = List.copyOf(blockers); }
    public record OrderView(UUID id, String orderNumber, UUID customerId, List<LineView> lines, MoneyView subtotal,
            MoneyView discount, MoneyView shippingFee, MoneyView total, UUID promotionId, String promotionCode,
            AddressView shippingAddress, AddressView billingAddress) { public OrderView { lines = List.copyOf(lines); } }
    public record LineView(UUID id, UUID variantId, String sku, String productName, String variantName, int quantity,
            MoneyView unitPriceAtOrder, MoneyView lineTotal) { }
    public record MoneyView(BigDecimal amount, String currency) { }
    public record AddressView(String label, String recipientName, String line1, String line2, String city, String region,
            String postalCode, String countryCode, String phone) { }
    public record ShippingOptionView(String id, String carrier, String serviceName, MoneyView fee,
            Instant estimatedDeliveryFrom, Instant estimatedDeliveryTo) { }
    public record Blocker(String code, String detail) { }
}

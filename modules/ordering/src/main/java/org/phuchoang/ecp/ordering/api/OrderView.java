package org.phuchoang.ecp.ordering.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Immutable API projection of values frozen when an order was placed. */
public record OrderView(UUID id, String orderNumber, UUID customerId, String status, String currency,
        List<LineView> lines, CheckoutView.MoneyView subtotal, CheckoutView.MoneyView discount,
        CheckoutView.MoneyView shippingFee, CheckoutView.MoneyView tax, CheckoutView.MoneyView total,
        UUID promotionId, String promotionCode, CheckoutView.AddressView shippingAddress,
        CheckoutView.AddressView billingAddress, Instant placedAt, Instant createdAt) {
    public OrderView { lines = List.copyOf(lines); }
    public record LineView(UUID id, UUID variantId, String sku, String productName, String variantName, int quantity,
            CheckoutView.MoneyView unitPriceAtOrder, CheckoutView.MoneyView lineDiscount,
            CheckoutView.MoneyView lineTotal) { }
}

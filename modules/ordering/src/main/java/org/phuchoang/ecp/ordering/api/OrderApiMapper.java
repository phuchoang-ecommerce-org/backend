package org.phuchoang.ecp.ordering.api;

import org.phuchoang.ecp.ordering.internal.domain.model.CheckoutAddress;
import org.phuchoang.ecp.ordering.internal.domain.model.CheckoutLine;
import org.phuchoang.ecp.ordering.internal.domain.model.Money;
import org.phuchoang.ecp.ordering.internal.domain.model.Order;
import org.springframework.stereotype.Component;

/** Keeps the public order snapshot independent from the mutable aggregate API. */
@Component
class OrderApiMapper {
    OrderView orderView(Order order) {
        return new OrderView(order.id(), order.orderNumber(), order.customerId(), order.status().name(),
            order.total().currency(), order.lines().stream().map(this::lineView).toList(), money(order.subtotal()),
            money(order.discount()), money(order.shippingOption().fee()), zero(order.total().currency()),
            money(order.total()), order.promotionId(), order.promotionCode(), address(order.shippingAddress()),
            address(order.billingAddress()), order.placedAt(), order.createdAt());
    }

    private OrderView.LineView lineView(CheckoutLine line) {
        var total = line.lineTotal();
        return new OrderView.LineView(line.id(), line.variantId(), line.sku(), line.productName(), line.variantName(),
            line.quantity(), money(line.unitPrice()), zero(total.currency()), money(total));
    }

    private CheckoutView.MoneyView money(Money value) {
        return new CheckoutView.MoneyView(value.amount().toPlainString(), value.currency());
    }

    private static CheckoutView.MoneyView zero(String currency) {
        return new CheckoutView.MoneyView("0.0000", currency);
    }

    private CheckoutView.AddressView address(CheckoutAddress value) {
        return new CheckoutView.AddressView(value.label(), value.recipientName(), value.line1(), value.line2(),
            value.city(), value.region(), value.postalCode(), value.countryCode(), value.phone());
    }
}

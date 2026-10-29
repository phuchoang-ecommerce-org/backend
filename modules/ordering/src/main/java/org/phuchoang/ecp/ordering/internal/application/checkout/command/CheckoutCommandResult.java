package org.phuchoang.ecp.ordering.internal.application.checkout.command;

import org.phuchoang.ecp.ordering.internal.domain.model.Checkout;
import org.phuchoang.ecp.ordering.internal.domain.model.CheckoutAddress;
import org.phuchoang.ecp.ordering.internal.domain.model.CheckoutLine;
import org.phuchoang.ecp.ordering.internal.domain.model.Money;
import org.phuchoang.ecp.ordering.internal.domain.model.ShippingOption;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Command response for a changed checkout; API maps this application contract to its public view. */
public record CheckoutCommandResult(CheckoutData checkout, List<ShippingOptionData> shippingOptions, boolean resumed) {
    public CheckoutCommandResult { shippingOptions = List.copyOf(shippingOptions); }

    public static CheckoutCommandResult from(Checkout checkout, List<ShippingOption> options, boolean resumed) {
        return new CheckoutCommandResult(data(checkout), options.stream().map(CheckoutCommandResult::option).toList(), resumed);
    }

    public static CheckoutData data(Checkout checkout) {
        return new CheckoutData(checkout.id(), checkout.orderNumber(), checkout.customerId(), checkout.lines().stream()
            .map(CheckoutCommandResult::line).toList(), "DRAFT", checkout.subtotal().currency(), money(checkout.subtotal()), money(checkout.discount()),
            money(checkout.shippingFee()), money(checkout.total()), checkout.promotionId(), checkout.promotionCode(),
            address(checkout.shippingAddress()), address(checkout.billingAddress()), checkout.shippingOption() == null ? null : option(checkout.shippingOption()),
            checkout.ready(), checkout.createdAt());
    }

    private static CheckoutLineData line(CheckoutLine line) {
        return new CheckoutLineData(line.id(), line.variantId(), line.sku(), line.productName(), line.variantName(), line.quantity(),
            money(line.unitPrice()), money(line.lineTotal()));
    }
    private static MoneyData money(Money value) { return new MoneyData(value.amount(), value.currency()); }
    private static CheckoutAddressData address(CheckoutAddress value) {
        return value == null ? null : new CheckoutAddressData(value.label(), value.recipientName(), value.line1(), value.line2(), value.city(),
            value.region(), value.postalCode(), value.countryCode(), value.phone());
    }
    private static ShippingOptionData option(ShippingOption value) {
        return new ShippingOptionData(value.id(), value.carrier(), value.serviceName(), money(value.fee()), value.estimatedDeliveryFrom(),
            value.estimatedDeliveryTo());
    }

    public record CheckoutData(UUID id, String orderNumber, UUID customerId, List<CheckoutLineData> lines, String status, String currency, MoneyData subtotal,
            MoneyData discount, MoneyData shippingFee, MoneyData total, UUID promotionId, String promotionCode,
            CheckoutAddressData shippingAddress, CheckoutAddressData billingAddress, ShippingOptionData shippingOption, boolean ready,
            Instant createdAt) {
        public CheckoutData { lines = List.copyOf(lines); }
    }
    public record CheckoutLineData(UUID id, UUID variantId, String sku, String productName, String variantName, int quantity,
            MoneyData unitPrice, MoneyData lineTotal) { }
    public record MoneyData(BigDecimal amount, String currency) { }
    public record CheckoutAddressData(String label, String recipientName, String line1, String line2, String city, String region,
            String postalCode, String countryCode, String phone) { }
    public record ShippingOptionData(String id, String carrier, String serviceName, MoneyData fee, Instant estimatedDeliveryFrom,
            Instant estimatedDeliveryTo) { }
}

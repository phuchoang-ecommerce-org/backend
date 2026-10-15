package org.phuchoang.ecp.ordering.internal.application.checkout.command;

import org.phuchoang.ecp.cart.api.CartLineView;
import org.phuchoang.ecp.cart.api.CartView;
import org.phuchoang.ecp.identity.api.view.AddressView;
import org.phuchoang.ecp.ordering.internal.domain.model.Checkout;
import org.phuchoang.ecp.ordering.internal.domain.model.CheckoutAddress;
import org.phuchoang.ecp.ordering.internal.domain.model.CheckoutLine;
import org.phuchoang.ecp.ordering.internal.domain.model.Money;
import org.phuchoang.ecp.ordering.internal.domain.model.ShippingOption;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class CheckoutCommandSupport {
    private CheckoutCommandSupport() { }

    public static List<CheckoutLine> purchasableLines(CartView cart) {
        return cart.lines().stream().filter(line -> line.purchasable() && !line.stockShort() && line.unitPrice() != null)
            .map(CheckoutCommandSupport::line).toList();
    }
    public static void requireEntireCartPurchasable(CartView cart, List<CheckoutLine> lines) {
        if (lines.isEmpty() || lines.size() != cart.lines().size()) throw new Checkout.NotPurchasable(unpurchasableDetail(cart));
    }
    public static CheckoutAddress address(CheckoutAddressInput input) {
        if (input == null) throw new IllegalArgumentException("addressId or address is required.");
        return new CheckoutAddress(input.label(), input.recipientName(), input.line1(), input.line2(), input.city(), input.region(),
            input.postalCode(), input.countryCode(), input.phone());
    }
    public static CheckoutAddress address(AddressView value) {
        return new CheckoutAddress(value.label(), value.recipientName(), value.line1(), value.line2(), value.city(), value.region(),
            value.postalCode(), value.countryCode(), value.phone());
    }
    public static ShippingOption provisionalQuote(CheckoutAddress address, String currency, Instant now) {
        return new ShippingOption("provisional-standard", "PROVISIONAL", "Standard", new Money(new BigDecimal("5.0000"), currency),
            now.plus(java.time.Duration.ofDays(3)), now.plus(java.time.Duration.ofDays(7)));
    }
    private static CheckoutLine line(CartLineView line) {
        return new CheckoutLine(line.id(), line.variantId(), line.sku(), line.productName(), line.variantName(), line.quantity(),
            new Money(line.unitPrice().amount(), line.unitPrice().currency()));
    }
    private static String unpurchasableDetail(CartView cart) {
        return cart.lines().stream().filter(line -> !line.purchasable() || line.stockShort() || line.unitPrice() == null)
            .map(line -> line.sku() + ": " + (line.unpurchasableReason() == null ? "insufficient stock" : line.unpurchasableReason()))
            .reduce("Cart contains no purchasable line.", (left, right) -> left + " " + right);
    }
}

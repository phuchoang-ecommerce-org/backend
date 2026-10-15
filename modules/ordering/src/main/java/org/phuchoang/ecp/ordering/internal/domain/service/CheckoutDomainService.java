package org.phuchoang.ecp.ordering.internal.domain.service;

import org.phuchoang.ecp.ordering.internal.domain.model.Checkout;
import org.phuchoang.ecp.ordering.internal.domain.repository.CheckoutRepository;

import org.phuchoang.ecp.ordering.internal.domain.model.CheckoutAddress;
import org.phuchoang.ecp.ordering.internal.domain.model.CheckoutLine;
import org.phuchoang.ecp.ordering.internal.domain.model.ShippingOption;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Domain-side aggregate persistence coordinator for checkout transitions. */
public class CheckoutDomainService {
    private final CheckoutRepository checkouts;

    public CheckoutDomainService(CheckoutRepository checkouts) {
        this.checkouts = checkouts;
    }

    public StartedCheckout startOrResume(UUID customerId, List<CheckoutLine> lines) {
        return checkouts.findDraftByCustomerId(customerId)
            .map(checkout -> new StartedCheckout(checkout, true))
            .orElseGet(() -> new StartedCheckout(Checkout.start(UUID.randomUUID(), customerId,
                "DRAFT-" + UUID.randomUUID(), lines).create(checkouts), false));
    }

    public Checkout setShippingAddress(UUID checkoutId, UUID customerId, CheckoutAddress address, List<ShippingOption> options) {
        return requireOwned(checkoutId, customerId).setShippingAddress(address, options, checkouts);
    }

    public Checkout setBillingAddress(UUID checkoutId, UUID customerId, CheckoutAddress address) {
        return requireOwned(checkoutId, customerId).setBillingAddress(address, checkouts);
    }

    public CheckoutAddress shippingAddress(UUID checkoutId, UUID customerId) {
        return requireOwned(checkoutId, customerId).shippingAddress();
    }

    public String currency(UUID checkoutId, UUID customerId) {
        return requireOwned(checkoutId, customerId).subtotal().currency();
    }

    public BigDecimal subtotal(UUID checkoutId, UUID customerId) {
        return requireOwned(checkoutId, customerId).subtotal().amount();
    }

    public List<CheckoutLine> lines(UUID checkoutId, UUID customerId) {
        return requireOwned(checkoutId, customerId).lines();
    }

    public Checkout selectShippingOption(UUID checkoutId, UUID customerId, ShippingOption option) {
        return requireOwned(checkoutId, customerId).selectShippingOption(option, checkouts);
    }

    public Checkout applyVoucher(UUID checkoutId, UUID customerId, UUID promotionId, String code, BigDecimal amount) {
        return requireOwned(checkoutId, customerId).applyVoucher(promotionId, code, amount, checkouts);
    }

    public void removeVoucher(UUID checkoutId, UUID customerId) {
        Checkout checkout = requireOwned(checkoutId, customerId);
        if (checkout.promotionId() != null) checkout.removeVoucher(checkouts);
    }

    public Checkout refresh(UUID checkoutId, UUID customerId, List<CheckoutLine> lines, String confirmationToken) {
        return requireOwned(checkoutId, customerId).refresh(lines, confirmationToken, checkouts);
    }

    /** Returns the aggregate snapshot that the placement domain transition will freeze. */
    public Checkout requireForPlacement(UUID checkoutId, UUID customerId) {
        return requireOwned(checkoutId, customerId);
    }

    private Checkout requireOwned(UUID checkoutId, UUID customerId) {
        Checkout checkout = checkouts.findById(checkoutId).orElseThrow(CheckoutNotFound::new);
        if (!checkout.customerId().equals(customerId)) throw new CheckoutNotFound();
        return checkout;
    }

    public static final class CheckoutNotFound extends RuntimeException { }
    public record StartedCheckout(Checkout checkout, boolean resumed) { }
}

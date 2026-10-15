package org.phuchoang.ecp.ordering.internal.application.checkout.command;

import org.phuchoang.ecp.ordering.internal.domain.model.Checkout;
import org.phuchoang.ecp.ordering.internal.domain.service.CheckoutDomainService;

/** Translates implementation exceptions before the API adapter applies its public error contract. */
public final class CheckoutFailures {
    private CheckoutFailures() { }
    public static CheckoutFailure map(RuntimeException exception) {
        if (exception instanceof CheckoutDomainService.CheckoutNotFound) return new CheckoutFailure(CheckoutFailure.Kind.NOT_FOUND, exception.getMessage());
        if (exception instanceof Checkout.NotPurchasable) return new CheckoutFailure(CheckoutFailure.Kind.NOT_PURCHASABLE, exception.getMessage());
        if (exception instanceof Checkout.DiscountExceedsSubtotal) return new CheckoutFailure(CheckoutFailure.Kind.DISCOUNT_EXCEEDS_SUBTOTAL, exception.getMessage());
        if (exception instanceof CheckoutCommandFailures.VoucherNotApplicable) return new CheckoutFailure(CheckoutFailure.Kind.VOUCHER_NOT_APPLICABLE, exception.getMessage());
        if (exception instanceof CheckoutCommandFailures.UnverifiedAccount) return new CheckoutFailure(CheckoutFailure.Kind.UNVERIFIED_ACCOUNT, exception.getMessage());
        return null;
    }
}

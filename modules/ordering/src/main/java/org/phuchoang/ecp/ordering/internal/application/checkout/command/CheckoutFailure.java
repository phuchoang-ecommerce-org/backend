package org.phuchoang.ecp.ordering.internal.application.checkout.command;

public record CheckoutFailure(Kind kind, String message) {
    public enum Kind { NOT_FOUND, NOT_PURCHASABLE, VOUCHER_NOT_APPLICABLE, DISCOUNT_EXCEEDS_SUBTOTAL, UNVERIFIED_ACCOUNT }
}

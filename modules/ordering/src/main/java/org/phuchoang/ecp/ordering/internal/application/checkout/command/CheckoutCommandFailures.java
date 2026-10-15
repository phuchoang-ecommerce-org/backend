package org.phuchoang.ecp.ordering.internal.application.checkout.command;

public final class CheckoutCommandFailures {
    private CheckoutCommandFailures() { }
    public static final class VoucherNotApplicable extends RuntimeException { }
    public static final class UnverifiedAccount extends RuntimeException { }
}

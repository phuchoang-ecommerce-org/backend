package org.phuchoang.ecp.ordering.internal.application.checkout.shipping.query;

import java.math.BigDecimal;

/** Minimal observation used for quoting, owned by the shipping-quote query. */
public record ShippingQuoteContext(String currency, boolean hasShippingAddress) { }

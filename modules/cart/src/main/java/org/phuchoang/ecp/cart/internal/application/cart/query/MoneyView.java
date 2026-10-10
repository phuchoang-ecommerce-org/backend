package org.phuchoang.ecp.cart.internal.application.cart.query;

import java.math.BigDecimal;

/** Live, advisory monetary display value; cart pricing is not persisted. */
public record MoneyView(BigDecimal amount, String currency) { }

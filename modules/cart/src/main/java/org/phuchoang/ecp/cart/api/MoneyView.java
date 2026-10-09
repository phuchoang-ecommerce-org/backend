package org.phuchoang.ecp.cart.api;

import java.math.BigDecimal;

/** A live, advisory amount; Cart never persists it. */
public record MoneyView(BigDecimal amount, String currency) { }

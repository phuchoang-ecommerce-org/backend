package org.phuchoang.ecp.catalog.api.view.common;

import java.math.BigDecimal;

/**
 * Wire-compatible money: amounts remain decimal strings rather than JSON numbers.
 *
 * @param amount exact decimal amount
 * @param currency ISO currency code
 */
public record MoneyView(
    /** Exact decimal amount, serialized as a JSON string. */ BigDecimal amount,
    /** ISO currency code associated with {@link #amount}. */ String currency) {
}

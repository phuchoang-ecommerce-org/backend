package org.phuchoang.ecp.catalog.api.view.common;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;

/**
 * Wire-compatible money: amounts remain decimal strings rather than JSON numbers.
 *
 * @param amount exact decimal amount
 * @param currency ISO currency code
 */
public record MoneyView(
    /** Exact decimal amount, serialized as a JSON string. */
    @JsonSerialize(using = ToStringSerializer.class) BigDecimal amount,
    /** ISO currency code associated with {@link #amount}. */ String currency) {
}

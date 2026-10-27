package org.phuchoang.ecp.cart.api;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;

/** A live, advisory amount; Cart never persists it. */
public record MoneyView(
    @JsonSerialize(using = ToStringSerializer.class) BigDecimal amount,
    String currency) { }

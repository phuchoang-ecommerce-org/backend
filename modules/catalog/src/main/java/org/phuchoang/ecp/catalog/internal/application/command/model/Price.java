package org.phuchoang.ecp.catalog.internal.application.command.model;

import java.math.BigDecimal;

/** Exact price input before it is validated and converted to the shared monetary value object. */
public record Price(BigDecimal amount, String currency) { }

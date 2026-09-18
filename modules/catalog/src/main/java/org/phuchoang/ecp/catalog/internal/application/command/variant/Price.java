package org.phuchoang.ecp.catalog.internal.application.command.variant;

import java.math.BigDecimal;

/** A requested replacement list price and the administration reason recorded with it. */
public record Price(BigDecimal amount, String currency, String reason) {
}

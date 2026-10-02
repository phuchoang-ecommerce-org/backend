package org.phuchoang.ecp.catalog.internal.application.productpricing;

import java.math.BigDecimal;

/** A read-side monetary amount; deliberately plain so cached projections serialise trivially. */
public record MoneyValue(BigDecimal amount, String currency) {
}

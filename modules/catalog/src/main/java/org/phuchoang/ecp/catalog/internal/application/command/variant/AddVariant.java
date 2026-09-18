package org.phuchoang.ecp.catalog.internal.application.command.variant;

import java.math.BigDecimal;
import java.util.Map;

/** Normalized input for adding one purchasable variant; the service assigns the identifier. */
public record AddVariant(String sku, String name, BigDecimal amount, String currency,
        Map<String, String> options, Integer weightGrams, boolean active) {
}

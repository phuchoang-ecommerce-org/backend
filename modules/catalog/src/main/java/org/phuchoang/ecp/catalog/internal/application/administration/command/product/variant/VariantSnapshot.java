package org.phuchoang.ecp.catalog.internal.application.administration.command.product.variant;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/** Application-owned representation of an administratively returned product variant. */
public record VariantSnapshot(UUID id, String sku, String name, BigDecimal amount, String currency,
                              Map<String, String> options, Integer weightGrams, boolean active) {
}

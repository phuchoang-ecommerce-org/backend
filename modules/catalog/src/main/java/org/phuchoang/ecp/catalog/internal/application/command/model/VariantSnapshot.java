package org.phuchoang.ecp.catalog.internal.application.command.model;

import org.phuchoang.ecp.catalog.internal.domain.model.Product;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/** Application-owned representation of an administratively returned product variant. */
public record VariantSnapshot(UUID id, String sku, String name, BigDecimal amount, String currency,
                              Map<String, String> options, Integer weightGrams, boolean active) {

    public static VariantSnapshot from(Product.Variant variant) {
        return new VariantSnapshot(variant.id(), variant.sku(), variant.name(), variant.amount(), variant.currency(),
            variant.options(), variant.weightGrams(), variant.active());
    }
}

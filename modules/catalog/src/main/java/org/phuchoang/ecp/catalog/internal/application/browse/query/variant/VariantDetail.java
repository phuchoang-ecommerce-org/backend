package org.phuchoang.ecp.catalog.internal.application.browse.query.variant;

import org.phuchoang.ecp.catalog.internal.application.productpricing.MoneyValue;

import java.util.Map;
import java.util.UUID;

/** A purchasable configuration as guests see it, including the advisory stock projection. */
public record VariantDetail(UUID id, String sku, String name, MoneyValue listPrice, MoneyValue promotionalPrice,
        Map<String, String> options, Integer weightGrams, boolean active, Boolean inStock) {
}

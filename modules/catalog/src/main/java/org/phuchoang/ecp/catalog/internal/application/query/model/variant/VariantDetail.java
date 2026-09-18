package org.phuchoang.ecp.catalog.internal.application.query.model.variant;

import org.phuchoang.ecp.catalog.internal.application.query.model.common.MoneyValue;

import java.util.Map;
import java.util.UUID;

/** A purchasable configuration as guests see it, including the advisory stock projection. */
public record VariantDetail(UUID id, String sku, String name, MoneyValue listPrice, MoneyValue promotionalPrice,
        Map<String, String> options, Integer weightGrams, boolean active, Boolean inStock) {
}

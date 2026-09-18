package org.phuchoang.ecp.catalog.internal.application.event.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;

import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record VariantPayload(UUID id, String sku, String name, MoneyPayload listPrice, Map<String, String> options,
        Integer weightGrams, boolean active) {

    public static VariantPayload of(Product.Variant variant) {
        return new VariantPayload(variant.id(), variant.sku(), variant.name(), MoneyPayload.of(variant),
            variant.options(), variant.weightGrams(), variant.active());
    }
}

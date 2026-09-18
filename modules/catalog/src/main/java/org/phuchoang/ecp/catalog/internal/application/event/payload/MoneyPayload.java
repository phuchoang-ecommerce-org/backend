package org.phuchoang.ecp.catalog.internal.application.event.payload;

import org.phuchoang.ecp.catalog.internal.domain.model.Product;

/** Money on the wire: the exact decimal as a plain string, never a float. */
public record MoneyPayload(String amount, String currency) {

    public static MoneyPayload of(Product.Variant variant) {
        return new MoneyPayload(variant.amount().toPlainString(), variant.currency());
    }
}

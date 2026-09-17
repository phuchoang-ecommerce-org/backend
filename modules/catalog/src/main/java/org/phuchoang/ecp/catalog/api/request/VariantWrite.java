package org.phuchoang.ecp.catalog.api.request;
import org.phuchoang.ecp.catalog.api.view.common.MoneyView;
import java.util.Map;

/** Administrator-supplied definition of a stock-keeping-unit variant owned by a product. */
public record VariantWrite(String sku, String name, MoneyView listPrice, Map<String, String> options, Integer weightGrams, Boolean active) { }

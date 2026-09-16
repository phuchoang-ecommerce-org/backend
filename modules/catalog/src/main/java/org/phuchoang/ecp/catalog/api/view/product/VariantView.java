package org.phuchoang.ecp.catalog.api.view.product;

import org.phuchoang.ecp.catalog.api.view.common.MoneyView;
import java.util.Map;
import java.util.UUID;

/**
 * Purchasable product configuration with its own SKU and price.
 *
 * @param id stable variant identifier
 * @param sku platform-wide stock-keeping unit
 * @param name variant display name
 * @param listPrice current list price
 * @param promotionalPrice temporary promotion-shaped price, absent until Promotion owns it
 * @param options dimension/value pairs that identify the configuration
 * @param weightGrams shippable weight in grams, when specified
 * @param active whether the variant is active for catalog use
 * @param availability advisory stock projection, or {@code null} when unknown
 */
public record VariantView(
    /** Stable variant identifier. */ UUID id,
    /** Platform-wide stock-keeping unit. */ String sku,
    /** Variant display name. */ String name,
    /** Current list price. */ MoneyView listPrice,
    /** Promotional price, absent until a promotion applies. */ MoneyView promotionalPrice,
    /** Dimension/value pairs that identify the configuration. */ Map<String, String> options,
    /** Shippable weight in grams, when specified. */ Integer weightGrams,
    /** Whether the variant is active for catalog use. */ boolean active,
    /** Advisory stock projection, or {@code null} while inventory availability is unknown. */ AdvisoryAvailabilityView availability) {
}

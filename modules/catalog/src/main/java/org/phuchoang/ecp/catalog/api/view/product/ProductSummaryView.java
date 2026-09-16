package org.phuchoang.ecp.catalog.api.view.product;

import org.phuchoang.ecp.catalog.api.view.common.MoneyView;
import java.util.UUID;

/**
 * Listing-shaped projection of a published product.
 *
 * @param id stable product identifier
 * @param name product display name
 * @param slug human-readable product identifier
 * @param brand product brand, when supplied
 * @param publicationStatus visibility state
 * @param primaryImageUrl optional primary product-image URL
 * @param priceFrom lowest active-variant price, or {@code null}
 * @param priceTo highest active-variant price, or {@code null}
 * @param averageRating denormalized review average, or {@code null}
 * @param reviewCount number of reviews represented by the average
 * @param inStock advisory stock state, or {@code null} when unknown
 */
public record ProductSummaryView(
    /** Stable product identifier. */ UUID id,
    /** Product display name. */ String name,
    /** Human-readable product identifier. */ String slug,
    /** Product brand, when supplied. */ String brand,
    /** Publication state; browse results contain only {@code PUBLISHED} products. */ String publicationStatus,
    /** Optional primary product-image URL. */ String primaryImageUrl,
    /** Lowest active-variant price, or {@code null} when no active variant has a price. */ MoneyView priceFrom,
    /** Highest active-variant price, or {@code null} when no active variant has a price. */ MoneyView priceTo,
    /** Denormalized review average, or {@code null} when no review exists. */ Double averageRating,
    /** Number of reviews represented by {@link #averageRating}. */ int reviewCount,
    /** Advisory stock state, or {@code null} while inventory availability is unknown. */ Boolean inStock) {
}

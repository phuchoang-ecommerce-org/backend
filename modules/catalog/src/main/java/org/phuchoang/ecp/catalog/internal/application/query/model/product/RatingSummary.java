package org.phuchoang.ecp.catalog.internal.application.query.model.product;

import java.util.Map;

/** Review aggregate for a product; empty until the review projection feeds it. */
public record RatingSummary(Double averageRating, int reviewCount, Map<String, Integer> distribution) {

    public static final RatingSummary EMPTY = new RatingSummary(null, 0,
        Map.of("1", 0, "2", 0, "3", 0, "4", 0, "5", 0));
}

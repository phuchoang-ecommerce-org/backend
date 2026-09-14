package org.phuchoang.ecp.catalog.api;

import java.util.Map;

/** The stable, intentionally empty rating shape until Review owns its projection. */
public record RatingSummaryView(Double averageRating, int reviewCount, Map<String, Integer> distribution) {
    public static final RatingSummaryView EMPTY = new RatingSummaryView(null, 0,
        Map.of("1", 0, "2", 0, "3", 0, "4", 0, "5", 0));
}

package org.phuchoang.ecp.catalog.internal.application.cache;

import java.time.Duration;

/**
 * TTLs are a backstop, not the mechanism (`ADR-0015` §4): event-driven invalidation through
 * {@link CatalogCacheInvalidation} is what keeps reads fresh; these bound the damage if it lags.
 */
public final class CatalogCachePolicy {

    public static final Duration BROWSE_TTL = Duration.ofMinutes(5);
    public static final Duration PRODUCT_DETAIL_TTL = Duration.ofMinutes(15);

    private CatalogCachePolicy() {
    }
}

package org.phuchoang.ecp.catalog.api;

import java.util.List;

/**
 * A cursor-paginated catalog listing.
 *
 * @param items products in the resolved page
 * @param nextCursor opaque cursor for the next page, or {@code null} at the end
 * @param total number of products matching the current query
 */
public record ProductPageView(
    /** Products in the resolved page. */ List<ProductSummaryView> items,
    /** Opaque cursor for the next page, or {@code null} when this is the final page. */ String nextCursor,
    /** Number of products matching the current query. */ long total) {
}

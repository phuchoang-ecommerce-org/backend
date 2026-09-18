package org.phuchoang.ecp.catalog.internal.application.query.model.product;

import java.util.List;

/** A keyset page of listing rows; {@code nextCursor} is {@code null} on the last page. */
public record ProductPage(List<ProductSummary> items, String nextCursor, long total) {
}

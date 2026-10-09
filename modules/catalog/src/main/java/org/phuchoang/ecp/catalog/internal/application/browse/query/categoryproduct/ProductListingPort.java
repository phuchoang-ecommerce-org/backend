package org.phuchoang.ecp.catalog.internal.application.browse.query.categoryproduct;

import org.phuchoang.ecp.catalog.internal.application.browse.query.categoryproduct.ProductListingQuery;
import org.phuchoang.ecp.catalog.internal.application.browse.query.categoryproduct.ProductPage;

import java.util.UUID;

/** Read port for keyset-paginated category listings. */
public interface ProductListingPort {

    /** Exact membership total for a category subtree and its filters (independent of cursor and sort). */
    long count(UUID categoryId, ProductListingQuery query);

    /** Published products in the category's subtree, filtered, sorted and paged per {@code query}. */
    ProductPage products(UUID categoryId, ProductListingQuery query, long total);
}

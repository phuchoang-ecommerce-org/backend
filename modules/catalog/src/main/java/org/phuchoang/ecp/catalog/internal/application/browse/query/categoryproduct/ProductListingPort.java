package org.phuchoang.ecp.catalog.internal.application.browse.query.categoryproduct;

import org.phuchoang.ecp.catalog.internal.application.browse.query.categoryproduct.ProductListingQuery;
import org.phuchoang.ecp.catalog.internal.application.browse.query.categoryproduct.ProductPage;

import java.util.UUID;

/** Read port for keyset-paginated product listings. A null category means the complete catalog. */
public interface ProductListingPort {

    /** Exact membership total for the optional category subtree and its filters. */
    long count(UUID categoryId, ProductListingQuery query);

    /** Published products in the optional category subtree, filtered, sorted and paged per {@code query}. */
    ProductPage products(UUID categoryId, ProductListingQuery query, long total);
}

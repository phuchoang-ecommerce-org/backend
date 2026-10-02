package org.phuchoang.ecp.catalog.internal.application.browse.categoryproduct;

import org.phuchoang.ecp.catalog.internal.application.browse.categoryproduct.ProductListingQuery;
import org.phuchoang.ecp.catalog.internal.application.browse.categoryproduct.ProductPage;

import java.util.UUID;

/** Read port for keyset-paginated category listings. */
public interface ProductListingPort {

    /** Published products in the category's subtree, filtered, sorted and paged per {@code query}. */
    ProductPage products(UUID categoryId, ProductListingQuery query);
}

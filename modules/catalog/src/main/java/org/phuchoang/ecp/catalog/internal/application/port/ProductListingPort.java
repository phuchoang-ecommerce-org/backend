package org.phuchoang.ecp.catalog.internal.application.port;

import org.phuchoang.ecp.catalog.internal.application.query.model.listing.ProductListingQuery;
import org.phuchoang.ecp.catalog.internal.application.query.model.product.ProductPage;

import java.util.UUID;

/** Read port for keyset-paginated category listings. */
public interface ProductListingPort {

    /** Published products in the category's subtree, filtered, sorted and paged per {@code query}. */
    ProductPage products(UUID categoryId, ProductListingQuery query);
}

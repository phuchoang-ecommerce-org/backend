package org.phuchoang.ecp.catalog.api.facade;

import org.phuchoang.ecp.catalog.api.view.product.ProductDetailView;
import org.phuchoang.ecp.catalog.api.view.product.RatingSummaryView;
import java.util.UUID;

/** Public product-detail read contract for the web composition root. */
public interface CatalogProductFacade {
    /** Redis cache key prefix for a product-detail projection. */
    String PRODUCT_CACHE_KEY_PREFIX = "cat:product:";

    /** Returns a published product or the uniform guest-facing not-found outcome. */
    ProductDetailView getProduct(UUID productId);

    /** Returns the designed empty rating shape for a published product. */
    RatingSummaryView getProductRatingSummary(UUID productId);
}

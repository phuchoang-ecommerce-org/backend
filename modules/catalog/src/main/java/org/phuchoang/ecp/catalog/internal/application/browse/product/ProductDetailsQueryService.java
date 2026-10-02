package org.phuchoang.ecp.catalog.internal.application.browse.product;

import org.phuchoang.ecp.catalog.internal.application.cache.CatalogCacheKeys;
import org.phuchoang.ecp.catalog.internal.application.cache.CatalogCachePolicy;
import org.phuchoang.ecp.catalog.internal.application.browse.CatalogReadErrors;
import org.phuchoang.ecp.catalog.internal.application.cache.CacheAside;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Product detail and rating reads, each backed by the published product source of truth. */
@Service
public class ProductDetailsQueryService {

    private final ProductDetailPort products;
    private final CacheAside cache;

    public ProductDetailsQueryService(ProductDetailPort products, CacheAside cache) {
        this.products = products;
        this.cache = cache;
    }

    public ProductDetail getProduct(UUID productId) {
        return cache.getOrLoad(CatalogCacheKeys.product(productId), CatalogCachePolicy.PRODUCT_DETAIL_TTL,
            () -> products.product(productId).orElseThrow(CatalogReadErrors::productNotFound), ProductDetail.class);
    }

    public RatingSummary getProductRatingSummary(UUID productId) {
        if (!products.publishedProductExists(productId)) {
            throw CatalogReadErrors.productNotFound();
        }
        return RatingSummary.EMPTY;
    }

}

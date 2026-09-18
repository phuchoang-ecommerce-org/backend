package org.phuchoang.ecp.catalog.internal.application.query.variant;

import org.phuchoang.ecp.catalog.internal.application.cache.CatalogCacheKeys;
import org.phuchoang.ecp.catalog.internal.application.cache.CatalogCachePolicy;
import org.phuchoang.ecp.catalog.internal.application.port.ProductDetailPort;
import org.phuchoang.ecp.catalog.internal.application.port.VariantBrowsePort;
import org.phuchoang.ecp.catalog.internal.application.query.CatalogReadErrors;
import org.phuchoang.ecp.catalog.internal.application.query.model.variant.VariantDetail;
import org.phuchoang.ecp.sharedkernel.api.cache.CacheAside;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Variant reads. Both operations first confirm the product is published: the API reports an
 * unpublished or absent product ("Product not found.") differently from a missing variant, and
 * `BR-CAT-02` forbids serving variants of an unpublished product even from a warm cache entry.
 */
@Service
public class VariantQueryService {

    private final ProductDetailPort products;
    private final VariantBrowsePort variants;
    private final CacheAside cache;

    public VariantQueryService(ProductDetailPort products, VariantBrowsePort variants, CacheAside cache) {
        this.products = products;
        this.variants = variants;
        this.cache = cache;
    }

    public List<VariantDetail> listProductVariants(UUID productId, Map<String, String> options) {
        requirePublished(productId);
        return variants.variants(productId, options);
    }

    public VariantDetail getProductVariant(UUID productId, UUID variantId) {
        requirePublished(productId);
        return cache.getOrLoad(CatalogCacheKeys.variant(variantId), CatalogCachePolicy.BROWSE_TTL,
            () -> variants.variant(productId, variantId).orElseThrow(CatalogReadErrors::variantNotFound),
            VariantDetail.class);
    }

    private void requirePublished(UUID productId) {
        if (!products.publishedProductExists(productId)) {
            throw CatalogReadErrors.productNotFound();
        }
    }
}

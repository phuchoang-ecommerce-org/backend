package org.phuchoang.ecp.catalog.internal.application.cache;

import org.phuchoang.ecp.sharedkernel.api.cache.CacheAside;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Semantic cache invalidation for Catalog: callers say what changed, this class knows which keys
 * that means. Driven by the Catalog events consumed in {@code app} (CQRS.md §6.3: a handler
 * {@code DEL}s, never writes), exposed to it through {@code catalog.api.CatalogCacheInvalidator}.
 */
@Component
public class CatalogCacheInvalidation {

    private final CacheAside cache;

    public CatalogCacheInvalidation(CacheAside cache) {
        this.cache = cache;
    }

    public void productChanged(UUID productId) {
        cache.invalidate(CatalogCacheKeys.product(productId));
    }

    public void variantsChanged(List<UUID> variantIds) {
        variantIds.forEach(variantId -> cache.invalidate(CatalogCacheKeys.variant(variantId)));
    }

    public void categoryTreeChanged() {
        cache.invalidate(CatalogCacheKeys.categoryTree());
    }

    public void categoryListingsChanged(List<UUID> categoryIds) {
        categoryIds.forEach(categoryId -> cache.invalidateByPrefix(CatalogCacheKeys.categoryListingPrefix(categoryId)));
    }
}

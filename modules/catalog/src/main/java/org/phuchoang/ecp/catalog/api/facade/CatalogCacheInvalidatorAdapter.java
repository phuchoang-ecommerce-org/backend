package org.phuchoang.ecp.catalog.api.facade;

import org.phuchoang.ecp.catalog.internal.application.cache.CatalogCacheInvalidation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
class CatalogCacheInvalidatorAdapter implements CatalogCacheInvalidator {

    private final CatalogCacheInvalidation invalidation;

    CatalogCacheInvalidatorAdapter(CatalogCacheInvalidation invalidation) {
        this.invalidation = invalidation;
    }

    @Override
    public void productChanged(UUID productId) {
        invalidation.productChanged(productId);
    }

    @Override
    public void variantsChanged(List<UUID> variantIds) {
        invalidation.variantsChanged(variantIds);
    }

    @Override
    public void categoryTreeChanged() {
        invalidation.categoryTreeChanged();
    }

    @Override
    public void categoryListingsChanged(List<UUID> categoryIds) {
        invalidation.categoryListingsChanged(categoryIds);
    }
}

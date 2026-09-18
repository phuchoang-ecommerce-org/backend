package org.phuchoang.ecp.catalog.internal.application.cache;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.sharedkernel.api.cache.CacheAside;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/** Population and invalidation must agree on every physical key (Backend Architecture.md §5.7). */
class CatalogCacheInvalidationTest {

    private final CacheAside cache = mock(CacheAside.class);
    private final CatalogCacheInvalidation invalidation = new CatalogCacheInvalidation(cache);

    @Test
    void keysMatchTheReadSideConventions() {
        UUID id = UUID.randomUUID();
        assertThat(CatalogCacheKeys.product(id)).isEqualTo("cat:product:" + id);
        assertThat(CatalogCacheKeys.variant(id)).isEqualTo("variant:" + id);
        assertThat(CatalogCacheKeys.categoryTree()).isEqualTo("category-tree");
        assertThat(CatalogCacheKeys.categoryListing(id, "fp")).isEqualTo("category-listing:" + id + ":fp")
            .startsWith(CatalogCacheKeys.categoryListingPrefix(id));
    }

    @Test
    void productChangeDeletesTheDetailEntry() {
        UUID productId = UUID.randomUUID();
        invalidation.productChanged(productId);
        verify(cache).invalidate("cat:product:" + productId);
        verifyNoMoreInteractions(cache);
    }

    @Test
    void variantChangesDeleteEachVariantEntry() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        invalidation.variantsChanged(List.of(a, b));
        verify(cache).invalidate("variant:" + a);
        verify(cache).invalidate("variant:" + b);
    }

    @Test
    void categoryChangesDeleteTheTreeAndEveryListingBeneathAffectedCategories() {
        UUID categoryId = UUID.randomUUID();
        invalidation.categoryTreeChanged();
        invalidation.categoryListingsChanged(List.of(categoryId));
        verify(cache).invalidate("category-tree");
        verify(cache).invalidateByPrefix("category-listing:" + categoryId + ":");
    }
}

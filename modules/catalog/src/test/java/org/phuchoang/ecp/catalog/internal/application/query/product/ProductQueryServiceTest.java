package org.phuchoang.ecp.catalog.internal.application.query.product;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.catalog.internal.application.port.CategoryBrowsePort;
import org.phuchoang.ecp.catalog.internal.application.port.ProductDetailPort;
import org.phuchoang.ecp.catalog.internal.application.port.ProductListingPort;
import org.phuchoang.ecp.catalog.internal.application.query.model.listing.ProductListingQuery;
import org.phuchoang.ecp.catalog.internal.application.query.model.product.ProductDetail;
import org.phuchoang.ecp.catalog.internal.application.query.model.product.ProductPage;
import org.phuchoang.ecp.catalog.internal.application.query.model.product.RatingSummary;
import org.phuchoang.ecp.sharedkernel.api.cache.CacheAside;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductQueryServiceTest {

    private final ProductDetailPort products = mock(ProductDetailPort.class);
    private final ProductListingPort listings = mock(ProductListingPort.class);
    private final CategoryBrowsePort categories = mock(CategoryBrowsePort.class);
    private final RecordingCache cache = new RecordingCache();
    private final ProductQueryService service = new ProductQueryService(products, listings, categories, cache);

    @Test
    void cachesPublishedDetailForFifteenMinutesUnderItsDocumentedKey() {
        UUID id = UUID.randomUUID();
        ProductDetail detail = detail(id);
        when(products.product(id)).thenReturn(Optional.of(detail));

        assertThat(service.getProduct(id)).isEqualTo(detail);

        assertThat(cache.key.get()).isEqualTo("cat:product:" + id);
        assertThat(cache.ttl.get()).isEqualTo(Duration.ofMinutes(15));
        verify(products).product(id);
    }

    @Test
    void absentAndUnpublishedDetailsHaveTheSameNotFoundOutcome() {
        UUID absent = UUID.randomUUID();
        UUID unpublished = UUID.randomUUID();
        when(products.product(absent)).thenReturn(Optional.empty());
        when(products.product(unpublished)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProduct(absent)).isInstanceOf(DomainException.class)
            .hasMessage("Product not found.");
        assertThatThrownBy(() -> service.getProduct(unpublished)).isInstanceOf(DomainException.class)
            .hasMessage("Product not found.");
    }

    @Test
    void returnsTheDesignedEmptyRatingSummaryOnlyForPublishedProducts() {
        UUID id = UUID.randomUUID();
        when(products.publishedProductExists(id)).thenReturn(true);

        assertThat(service.getProductRatingSummary(id)).isEqualTo(RatingSummary.EMPTY);
    }

    @Test
    void listingsAreCachedBeneathTheCategoryPrefixTheInvalidatorUses() {
        UUID categoryId = UUID.randomUUID();
        ProductListingQuery query = new ProductListingQuery(null, 20, "default", List.of(), null, null, null);
        ProductPage page = new ProductPage(List.of(), null, 0);
        when(categories.categoryExists(categoryId)).thenReturn(true);
        when(listings.products(categoryId, query)).thenReturn(page);

        assertThat(service.listCategoryProducts(categoryId, query)).isEqualTo(page);

        assertThat(cache.key.get()).startsWith("category-listing:" + categoryId + ":");
        assertThat(cache.ttl.get()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void anUnknownCategoryIsNotFoundRatherThanAnEmptyListing() {
        UUID categoryId = UUID.randomUUID();
        when(categories.categoryExists(categoryId)).thenReturn(false);

        assertThatThrownBy(() -> service.listCategoryProducts(categoryId,
            new ProductListingQuery(null, 20, "default", List.of(), null, null, null)))
            .isInstanceOf(DomainException.class).hasMessage("Category not found.");
    }

    private static ProductDetail detail(UUID id) {
        return new ProductDetail(id, "Product", "product", null, null, "PUBLISHED", null, List.of(), Map.of(),
            List.of(), List.of(), null, 0);
    }

    private static final class RecordingCache implements CacheAside {
        private final AtomicReference<String> key = new AtomicReference<>();
        private final AtomicReference<Duration> ttl = new AtomicReference<>();

        @Override
        public <T> T getOrLoad(String key, Duration ttl, Supplier<T> loader, Class<T> type) {
            this.key.set(key);
            this.ttl.set(ttl);
            return loader.get();
        }

        @Override
        public void invalidate(String key) {
        }

        @Override
        public void invalidateByPrefix(String prefix) {
        }
    }
}

package org.phuchoang.ecp.catalog.application.query;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.catalog.application.port.CatalogBrowsePort;
import org.phuchoang.ecp.sharedkernel.api.CacheAside;
import org.phuchoang.ecp.sharedkernel.api.DomainException;

import java.time.Duration;
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

class CatalogProductServiceTest {

    @Test
    void cachesPublishedDetailForFifteenMinutesUnderItsDocumentedKey() {
        CatalogBrowsePort repository = mock(CatalogBrowsePort.class);
        UUID id = UUID.randomUUID();
        CatalogBrowseModel.ProductDetail detail = detail(id);
        when(repository.product(id)).thenReturn(Optional.of(detail));
        RecordingCache cache = new RecordingCache();

        assertThat(new CatalogProductService(repository, cache).getProduct(id)).isEqualTo(detail);

        assertThat(cache.key.get()).isEqualTo("cat:product:" + id);
        assertThat(cache.ttl.get()).isEqualTo(Duration.ofMinutes(15));
        verify(repository).product(id);
    }

    @Test
    void absentAndUnpublishedDetailsHaveTheSameNotFoundOutcome() {
        CatalogBrowsePort repository = mock(CatalogBrowsePort.class);
        UUID absent = UUID.randomUUID();
        UUID unpublished = UUID.randomUUID();
        when(repository.product(absent)).thenReturn(Optional.empty());
        when(repository.product(unpublished)).thenReturn(Optional.empty());
        CatalogProductService service = new CatalogProductService(repository, new RecordingCache());

        assertThatThrownBy(() -> service.getProduct(absent)).isInstanceOf(DomainException.class)
            .hasMessage("Product not found.");
        assertThatThrownBy(() -> service.getProduct(unpublished)).isInstanceOf(DomainException.class)
            .hasMessage("Product not found.");
    }

    @Test
    void returnsTheDesignedEmptyRatingSummaryOnlyForPublishedProducts() {
        CatalogBrowsePort repository = mock(CatalogBrowsePort.class);
        UUID id = UUID.randomUUID();
        when(repository.publishedProductExists(id)).thenReturn(true);

        assertThat(new CatalogProductService(repository, new RecordingCache()).getProductRatingSummary(id))
            .isEqualTo(CatalogBrowseModel.RatingSummary.EMPTY);
    }

    private static CatalogBrowseModel.ProductDetail detail(UUID id) {
        return new CatalogBrowseModel.ProductDetail(id, "Product", "product", null, null, "PUBLISHED", null,
            java.util.List.of(), Map.of(), java.util.List.of(), java.util.List.of(), null, 0);
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
    }
}

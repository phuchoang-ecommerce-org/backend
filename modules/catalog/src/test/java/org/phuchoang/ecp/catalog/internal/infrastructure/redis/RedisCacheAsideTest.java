package org.phuchoang.ecp.catalog.internal.infrastructure.redis;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.catalog.internal.application.browse.category.CategoryNode;
import org.phuchoang.ecp.catalog.internal.application.browse.categoryproduct.ProductPage;
import org.phuchoang.ecp.catalog.internal.application.browse.product.ProductDetail;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisCacheAsideTest {

    @Test
    @SuppressWarnings("unchecked")
    void deserializesEveryCatalogBrowseResultIntoItsRequestedTypeAndCountsWarmReadsAsHits() {
        RedisTemplate<String, String> template = mock(RedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        Map<String, String> stored = new ConcurrentHashMap<>();
        when(template.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenAnswer(invocation -> stored.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            stored.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(values).set(anyString(), anyString(), any(Duration.class));
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        RedisCacheAside cache = new RedisCacheAside(template, meters, JsonMapper.builder().build());

        assertCachedOnce(cache, "category-tree", categoryTree(), CategoryNode.class);
        assertCachedOnce(cache, "category-listing:products", new ProductPage(List.of(), null, 0), ProductPage.class);
        assertCachedOnce(cache, "cat:product:product", detail(), ProductDetail.class);

        assertThat(meters.find("ecp.catalog.cache.accesses").tags("cache", "catalog", "result", "hit")
            .counter().count()).isEqualTo(3);
        assertThat(meters.find("ecp.catalog.cache.accesses").tags("cache", "catalog", "result", "miss")
            .counter().count()).isEqualTo(3);
    }

    private static <T> void assertCachedOnce(RedisCacheAside cache, String key, T expected, Class<T> type) {
        AtomicInteger loads = new AtomicInteger();

        T first = cache.getOrLoad(key, Duration.ofMinutes(1), () -> {
            loads.incrementAndGet();
            return expected;
        }, type);
        T second = cache.getOrLoad(key, Duration.ofMinutes(1), () -> {
            loads.incrementAndGet();
            return expected;
        }, type);

        assertThat(first).isEqualTo(expected);
        assertThat(second).isEqualTo(expected);
        assertThat(loads).hasValue(1);
    }

    private static CategoryNode categoryTree() {
        return new CategoryNode(UUID.randomUUID(), null, "Root", "root", 0, 0, null, false, List.of(), List.of());
    }

    private static ProductDetail detail() {
        return new ProductDetail(UUID.randomUUID(), "Product", "product", null, null, "PUBLISHED", null, List.of(),
            Map.of(), List.of(), List.of(), 4.5, 2);
    }
}

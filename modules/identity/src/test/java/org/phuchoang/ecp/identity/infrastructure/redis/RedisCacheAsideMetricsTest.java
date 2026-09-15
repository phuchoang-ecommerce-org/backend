package org.phuchoang.ecp.identity.infrastructure.redis;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisCacheAsideMetricsTest {

    @Test
    void recordsTheDocumentedCatalogCacheHitMeterAndFixedTags() {
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("cat:product:abc")).thenReturn("cached");
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        assertThat(new RedisCacheAside(redis, registry).getOrLoad("cat:product:abc", java.time.Duration.ofMinutes(15),
            () -> "loaded", String.class)).isEqualTo("cached");

        assertThat(registry.get("ecp.catalog.cache.accesses").tags("cache", "catalog", "result", "hit")
            .counter().count()).isEqualTo(1);
    }
}

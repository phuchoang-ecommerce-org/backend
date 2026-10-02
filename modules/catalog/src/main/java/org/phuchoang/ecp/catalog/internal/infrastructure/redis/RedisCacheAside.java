package org.phuchoang.ecp.catalog.internal.infrastructure.redis;

import io.micrometer.core.instrument.MeterRegistry;
import org.phuchoang.ecp.catalog.internal.application.cache.CacheAside;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * The cache-aside helper (`EN-WIRE-2`, Backend Architecture.md §5.6), backed by {@code
 * redis-cache} — evictable, safe to flush at any time (`ADR-0015` §4 rule 1). Not called by any
 * module yet this sprint; it exists ready for `US-AUD-04` (Sprint 04) and later catalog caching,
 * per the sprint backlog's own wording. TTL is jittered ±10% so many keys expiring together don't
 * all miss at once.
 */
@Component
class RedisCacheAside implements CacheAside {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheAside.class);
    private static final int SCAN_BATCH = 500;

    private final RedisTemplate<String, String> cacheRedisTemplate;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper json;

    RedisCacheAside(@Qualifier("cacheRedisTemplate") RedisTemplate<String, String> cacheRedisTemplate,
                    MeterRegistry meterRegistry, ObjectMapper json) {
        this.cacheRedisTemplate = cacheRedisTemplate;
        this.meterRegistry = meterRegistry;
        this.json = json;
    }

    @Override
    public <T> T getOrLoad(String key, Duration ttl, Supplier<T> loader, Class<T> type) {
        String cached;
        try {
            cached = cacheRedisTemplate.opsForValue().get(key);
        } catch (DataAccessException e) {
            recordCatalogAccess(key, "error");
            log.warn("redis-cache read failed for {}; falling through to loader (a cache error is a miss)", key, e);
            cached = null;
        }
        if (cached != null) {
            try {
                T value = json.readValue(cached, type);
                recordCatalogAccess(key, "hit");
                return value;
            } catch (Exception e) {
                // Entries written with the earlier generic serializer (or a partially-written value)
                // are ordinary cache misses. Replacing them keeps the cache safe to flush and migrate.
                recordCatalogAccess(key, "miss");
                log.warn("redis-cache value for {} is incompatible with {}; replacing it", key, type.getSimpleName());
            }
        } else {
            recordCatalogAccess(key, "miss");
        }

        T loaded = loader.get();
        try {
            double jitter = 0.9 + ThreadLocalRandom.current().nextDouble(0.2); // ±10%
            cacheRedisTemplate.opsForValue().set(key, json.writeValueAsString(loaded),
                Duration.ofMillis((long) (ttl.toMillis() * jitter)));
        } catch (Exception e) {
            log.warn("redis-cache write failed for {}; the value is still returned to the caller", key, e);
        }
        return loaded;
    }

    @Override
    public void invalidate(String key) {
        try {
            cacheRedisTemplate.delete(key);
        } catch (DataAccessException e) {
            log.warn("redis-cache DEL failed for {}", key, e);
        }
    }

    @Override
    public void invalidateByPrefix(String prefix) {
        ScanOptions options = ScanOptions.scanOptions().match(prefix + "*").count(SCAN_BATCH).build();
        try (Cursor<String> cursor = cacheRedisTemplate.scan(options)) {
            List<String> batch = new ArrayList<>(SCAN_BATCH);
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() == SCAN_BATCH) {
                    cacheRedisTemplate.unlink(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                cacheRedisTemplate.unlink(batch);
            }
        } catch (DataAccessException e) {
            log.warn("redis-cache SCAN/UNLINK failed for prefix {}", prefix, e);
        }
    }

    private void recordCatalogAccess(String key, String result) {
        if (key.equals("category-tree") || key.startsWith("category-tree:") || key.startsWith("category-listing:") || key.startsWith("variant:")
                || key.startsWith("cat:product:")) {
            meterRegistry.counter("ecp.catalog.cache.accesses", "cache", "catalog", "result", result).increment();
        }
    }
}

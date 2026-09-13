package org.phuchoang.ecp.sharedkernel.api;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * The cache-aside pattern (Backend Architecture.md §5.6), backed by {@code redis-cache}
 * (ADR-0034) — evictable, no persistence, safe to flush entirely at any time. A cache miss or a
 * cache-backend error both fall through to {@code loader}; this port never throws for a cache
 * problem, because a cache is only ever an optimisation (ADR-0015 §4 rule 1).
 */
public interface CacheAside {

    /**
     * Returns the cached value under {@code key}, or computes it with {@code loader}, stores it
     * with {@code ttl} (jittered per §5.6), and returns it. A cache error is treated as a miss.
     */
    <T> T getOrLoad(String key, Duration ttl, Supplier<T> loader, Class<T> type);

    /** Removes {@code key}. {@code DEL}, never a value overwrite. */
    void invalidate(String key);
}

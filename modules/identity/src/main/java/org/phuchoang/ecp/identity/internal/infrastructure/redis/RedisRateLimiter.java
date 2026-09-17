package org.phuchoang.ecp.identity.internal.infrastructure.redis;

import org.phuchoang.ecp.sharedkernel.api.ratelimit.RateLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

/**
 * The sliding-window limiter (Backend Architecture.md §5.8), backed by {@code redis-state} — the
 * bucket counters are correctness-bearing state, never a cache (`ADR-0034`). One atomic Lua
 * script does the read-decide-write; a `RateLimiter.RateLimiterUnavailableException` on any
 * Redis error lets the caller (a {@code RateLimitFilter}) decide fail-open vs fail-closed per
 * bucket — this adapter never decides that itself.
 */
@Component
class RedisRateLimiter implements RateLimiter {

    private static final DefaultRedisScript<List> SLIDING_WINDOW_SCRIPT = new DefaultRedisScript<>("""
        redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[1] - ARGV[2])
        local used = redis.call('ZCARD', KEYS[1])
        if used >= tonumber(ARGV[3]) then
          local oldest = redis.call('ZRANGE', KEYS[1], 0, 0, 'WITHSCORES')
          return { 0, math.ceil((tonumber(oldest[2]) + ARGV[2] - ARGV[1]) / 1000) }
        end
        redis.call('ZADD', KEYS[1], ARGV[1], ARGV[4])
        redis.call('PEXPIRE', KEYS[1], ARGV[2])
        return { 1, 0 }
        """, List.class);

    private final RedisTemplate<String, String> stateRedisTemplate;
    private final RateLimitBuckets rateLimitBuckets;
    private final Clock clock;

    RedisRateLimiter(@Qualifier("stateRedisTemplate") RedisTemplate<String, String> stateRedisTemplate,
            RateLimitBuckets rateLimitBuckets, Clock clock) {
        this.stateRedisTemplate = stateRedisTemplate;
        this.rateLimitBuckets = rateLimitBuckets;
        this.clock = clock;
    }

    @Override
    public Decision tryConsume(String bucket, String callerId) {
        // Key order matches Database.md §7.3's `rl:{callerId}:{bucket}` (US-AUD-04 fix — this
        // adapter previously had the two segments swapped).
        String key = "rl:" + callerId + ":" + bucket;
        RateLimitBucket limits = rateLimitBuckets.of(bucket);
        try {
            long nowMs = clock.millis();
            List<Long> result = stateRedisTemplate.execute(SLIDING_WINDOW_SCRIPT, List.of(key),
                String.valueOf(nowMs), String.valueOf(limits.windowMillis()), String.valueOf(limits.limit()),
                nowMs + "-" + Math.random());
            boolean allowed = result != null && result.get(0) == 1L;
            long retryAfter = result == null ? limits.windowSeconds() : result.get(1);
            return new Decision(allowed, retryAfter);
        } catch (DataAccessException e) {
            throw new RateLimiterUnavailableException("redis-state unavailable for bucket " + bucket, e);
        }
    }
}

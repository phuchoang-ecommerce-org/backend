package org.phuchoang.ecp.identity.internal.infrastructure.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * The four rate-limit buckets (`Backend Architecture.md` §5.8's table, `NFR-SEC-05`), bound from
 * {@code ecp.rate-limit.*} in {@code application.yml} — {@code auth-strict} and
 * {@code payment-retry} fail closed (brute-force/fraud defence); {@code write} and {@code read}
 * fail open (a throttle, not a gate). An unrecognised bucket name falls back to {@code write}'s
 * numbers rather than throwing — a rate limiter is defence, not a place to add a new way for a
 * typo'd bucket name to become a 500 (`US-AUD-04`).
 */
@Component
class RateLimitBuckets {

    private final Map<String, RateLimitBucket> buckets;

    RateLimitBuckets(
            @Value("${ecp.rate-limit.auth-strict.limit:10}") int authStrictLimit,
            @Value("${ecp.rate-limit.auth-strict.window-seconds:300}") int authStrictWindowSeconds,
            @Value("${ecp.rate-limit.write.limit:120}") int writeLimit,
            @Value("${ecp.rate-limit.write.window-seconds:60}") int writeWindowSeconds,
            @Value("${ecp.rate-limit.payment-retry.limit:5}") int paymentRetryLimit,
            @Value("${ecp.rate-limit.payment-retry.window-seconds:900}") int paymentRetryWindowSeconds,
            @Value("${ecp.rate-limit.read.limit:600}") int readLimit,
            @Value("${ecp.rate-limit.read.window-seconds:60}") int readWindowSeconds) {
        this.buckets = Map.of(
            "auth-strict", new RateLimitBucket(authStrictLimit, authStrictWindowSeconds),
            "write", new RateLimitBucket(writeLimit, writeWindowSeconds),
            "payment-retry", new RateLimitBucket(paymentRetryLimit, paymentRetryWindowSeconds),
            "read", new RateLimitBucket(readLimit, readWindowSeconds));
    }

    RateLimitBucket of(String bucketName) {
        return buckets.getOrDefault(bucketName, buckets.get("write"));
    }
}

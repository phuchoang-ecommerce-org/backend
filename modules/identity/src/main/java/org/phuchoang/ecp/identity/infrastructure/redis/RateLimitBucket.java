package org.phuchoang.ecp.identity.infrastructure.redis;

/**
 * A bucket's configured limit/window (`Backend Architecture.md` §5.8's table, `NFR-SEC-05`). The
 * four buckets and their numbers are configured via {@code ecp.rate-limit.*} in
 * {@code application.yml} and bound by {@link RateLimitBuckets}.
 */
record RateLimitBucket(int limit, int windowSeconds) {

    long windowMillis() {
        return windowSeconds * 1000L;
    }
}

package org.phuchoang.ecp.identity.infrastructure.redis;

/**
 * A bucket's configured limit/window (Backend Architecture.md §5.8's table — `ecp.rate-limit.*` in
 * {@code application.yml}). Unrecognised bucket names fall back to the {@code write} bucket's
 * (fail-open) numbers rather than throwing, since a rate limiter is defence, not a place to add a
 * new way for a typo'd bucket name to become a 500.
 */
record RateLimitBucket(int limit, int windowSeconds) {

    private static final RateLimitBucket AUTH_STRICT = new RateLimitBucket(10, 300);
    private static final RateLimitBucket WRITE = new RateLimitBucket(120, 60);

    static RateLimitBucket of(String bucketName) {
        return "auth-strict".equals(bucketName) ? AUTH_STRICT : WRITE;
    }

    long windowMillis() {
        return windowSeconds * 1000L;
    }
}

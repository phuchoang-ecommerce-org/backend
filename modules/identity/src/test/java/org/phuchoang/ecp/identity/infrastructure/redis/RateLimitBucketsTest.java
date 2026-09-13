package org.phuchoang.ecp.identity.infrastructure.redis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** L1 — `US-AUD-04`: the four configured buckets, and the write-bucket fallback for a typo'd name. */
class RateLimitBucketsTest {

    private final RateLimitBuckets buckets = new RateLimitBuckets(10, 300, 120, 60, 5, 900, 600, 60);

    @Test
    void resolvesEachOfTheFourConfiguredBuckets() {
        assertThat(buckets.of("auth-strict")).isEqualTo(new RateLimitBucket(10, 300));
        assertThat(buckets.of("write")).isEqualTo(new RateLimitBucket(120, 60));
        assertThat(buckets.of("payment-retry")).isEqualTo(new RateLimitBucket(5, 900));
        assertThat(buckets.of("read")).isEqualTo(new RateLimitBucket(600, 60));
    }

    @Test
    void anUnrecognisedBucketNameFallsBackToWrite() {
        assertThat(buckets.of("not-a-real-bucket")).isEqualTo(buckets.of("write"));
    }
}

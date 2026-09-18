package org.phuchoang.ecp.configuration.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** {@code ecp.redis.*}: the two Redis instances of the `ADR-0034` topology, never one. */
@ConfigurationProperties("ecp.redis")
public record RedisProperties(@DefaultValue Cache cache, @DefaultValue State state) {

    /** {@code redis-cache}: evictable, no persistence, safe to flush — availability-oriented. */
    public record Cache(@DefaultValue("localhost") String host, @DefaultValue("6379") int port) {
    }

    /** {@code redis-state}: rate-limit windows and other correctness-sensitive state. */
    public record State(@DefaultValue("localhost") String host, @DefaultValue("6380") int port) {
    }
}

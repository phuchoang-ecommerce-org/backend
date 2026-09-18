package org.phuchoang.ecp.messaging.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code ecp.outbox.*}. {@code pollDelayMs} is bound here for documentation and validation; the
 * scheduler itself reads it through {@code ${ecp.outbox.poll-delay-ms}} because
 * {@code @Scheduled} takes a property placeholder, not a bean.
 */
@ConfigurationProperties("ecp.outbox")
public record OutboxProperties(@DefaultValue("100") int batchSize, @DefaultValue("1000") long pollDelayMs) {

    public OutboxProperties {
        if (batchSize < 1) {
            throw new IllegalArgumentException("ecp.outbox.batch-size must be at least 1.");
        }
        if (pollDelayMs < 1) {
            throw new IllegalArgumentException("ecp.outbox.poll-delay-ms must be at least 1.");
        }
    }
}

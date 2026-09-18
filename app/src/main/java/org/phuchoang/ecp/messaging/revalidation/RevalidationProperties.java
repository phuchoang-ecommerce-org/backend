package org.phuchoang.ecp.messaging.revalidation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code ecp.revalidation.*}: the storefront callback the Catalog revalidation consumer forwards
 * to. Deliberately not validated at startup — a deployment without a storefront still boots and
 * serves the API; an unconfigured callback fails each consumed event instead (redelivered by
 * Kafka), exactly as before.
 */
@ConfigurationProperties("ecp.revalidation")
public record RevalidationProperties(@DefaultValue("") String url, @DefaultValue("") String secret,
        @DefaultValue("ecp.web-revalidation") String consumerGroup) {

    public boolean isConfigured() {
        return !url.isBlank() && !secret.isBlank();
    }
}

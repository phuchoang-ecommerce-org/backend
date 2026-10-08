package org.phuchoang.ecp.inventory.internal.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Retry policy for optimistic-lock conflicts while resolving an Inventory reservation.
 *
 * <p>The policy is infrastructure configuration rather than a domain rule: a retry must never
 * change the all-or-nothing reservation semantics owned by {@code StockItem}.
 */
@ConfigurationProperties("ecp.inventory.reservation")
public record InventoryReservationProperties(@DefaultValue("8") int maxAttempts) {

    public InventoryReservationProperties {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("ecp.inventory.reservation.max-attempts must be at least 1.");
        }
    }
}

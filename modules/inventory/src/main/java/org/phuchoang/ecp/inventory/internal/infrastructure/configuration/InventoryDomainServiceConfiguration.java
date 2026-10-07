package org.phuchoang.ecp.inventory.internal.infrastructure.configuration;

import org.phuchoang.ecp.inventory.internal.application.command.reservation.ReservationRetryPolicy;
import org.phuchoang.ecp.inventory.internal.domain.policy.StockAllocationPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Makes Inventory's allocation policy available to its allocation application use case. */
@Configuration(proxyBeanMethods = false)
public class InventoryDomainServiceConfiguration {

    @Bean
    StockAllocationPolicy stockAllocationPolicy() {
        return new StockAllocationPolicy();
    }

    @Bean
    ReservationRetryPolicy reservationRetryPolicy(InventoryReservationProperties properties) {
        return new ReservationRetryPolicy(properties.maxAttempts());
    }
}

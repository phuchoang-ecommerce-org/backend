package org.phuchoang.ecp.inventory.internal.infrastructure.configuration;

import org.phuchoang.ecp.inventory.internal.application.command.reservation.ReservationRetryPolicy;
import org.phuchoang.ecp.inventory.internal.domain.service.CommitStockReservationDomainService;
import org.phuchoang.ecp.inventory.internal.domain.service.ReleaseStockReservationDomainService;
import org.phuchoang.ecp.inventory.internal.domain.service.ReserveStockDomainService;
import org.phuchoang.ecp.inventory.internal.domain.service.StockAllocationPolicy;
import org.phuchoang.ecp.inventory.internal.domain.repository.StockItemRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Makes Inventory's allocation policy available to its allocation application use case. */
@Configuration(proxyBeanMethods = false)
public class InventoryDomainServiceConfiguration {

    @Bean
    StockAllocationPolicy stockAllocationPolicy() {
        return new StockAllocationPolicy();
    }

    @Bean
    ReserveStockDomainService reserveStockDomainService(StockItemRepository stockItems) {
        return new ReserveStockDomainService(stockItems);
    }

    @Bean
    CommitStockReservationDomainService commitStockReservationDomainService(StockItemRepository stockItems,
            Clock clock) {
        return new CommitStockReservationDomainService(stockItems, clock);
    }

    @Bean
    ReleaseStockReservationDomainService releaseStockReservationDomainService(StockItemRepository stockItems,
            Clock clock) {
        return new ReleaseStockReservationDomainService(stockItems, clock);
    }

    @Bean
    ReservationRetryPolicy reservationRetryPolicy(InventoryReservationProperties properties) {
        return new ReservationRetryPolicy(properties.maxAttempts());
    }
}

package org.phuchoang.ecp.ordering.internal.infrastructure.configuration;

import org.phuchoang.ecp.ordering.internal.domain.repository.CheckoutRepository;
import org.phuchoang.ecp.ordering.internal.domain.repository.OrderRepository;
import org.phuchoang.ecp.ordering.internal.domain.service.CheckoutDomainService;
import org.phuchoang.ecp.ordering.internal.domain.service.PlaceOrderDomainService;
import org.phuchoang.ecp.ordering.internal.domain.service.OrderLifecycleDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Spring wiring for Ordering's persistence-backed Checkout domain service. */
@Configuration
class CheckoutPersistenceConfiguration {
    @Bean
    CheckoutDomainService checkoutDomainService(CheckoutRepository checkouts) {
        return new CheckoutDomainService(checkouts);
    }

    @Bean
    PlaceOrderDomainService placeOrderDomainService(OrderRepository orders) {
        return new PlaceOrderDomainService(orders);
    }

    @Bean
    OrderLifecycleDomainService orderLifecycleDomainService(OrderRepository orders) {
        return new OrderLifecycleDomainService(orders);
    }
}

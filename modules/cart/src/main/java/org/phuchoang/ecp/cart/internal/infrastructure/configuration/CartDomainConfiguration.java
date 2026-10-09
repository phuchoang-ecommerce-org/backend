package org.phuchoang.ecp.cart.internal.infrastructure.configuration;

import org.phuchoang.ecp.cart.internal.domain.repository.CartRepository;
import org.phuchoang.ecp.cart.internal.domain.service.CartCommandService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Runtime wiring only; the command service itself remains framework independent. */
@Configuration(proxyBeanMethods = false)
class CartDomainConfiguration {
    @Bean
    CartCommandService cartCommandService(CartRepository carts) {
        return new CartCommandService(carts);
    }
}

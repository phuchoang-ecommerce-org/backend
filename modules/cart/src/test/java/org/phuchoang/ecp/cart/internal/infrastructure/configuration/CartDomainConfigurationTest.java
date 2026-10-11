package org.phuchoang.ecp.cart.internal.infrastructure.configuration;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.cart.internal.domain.policy.CartMergePolicy;
import org.phuchoang.ecp.cart.internal.domain.repository.CartRepository;
import org.phuchoang.ecp.cart.internal.domain.service.CartCommandService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CartDomainConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(CartDomainConfiguration.class)
        .withBean(CartRepository.class, () -> mock(CartRepository.class))
        .withBean(CartLifetimeProperties.class,
            () -> new CartLifetimeProperties(Duration.ofDays(7), Duration.ofDays(30), Duration.ofMinutes(1)));

    @Test
    void providesTheMergePolicyRequiredByTheAggregateCommandService() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(CartMergePolicy.class);
            assertThat(context).hasSingleBean(CartCommandService.class);
        });
    }
}

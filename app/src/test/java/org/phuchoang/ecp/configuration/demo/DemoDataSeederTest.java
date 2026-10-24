package org.phuchoang.ecp.configuration.demo;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class DemoDataSeederTest {

    @Test
    void productionDoesNotRegisterTheSeederEvenIfDevIsAlsoActive() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("prod", "dev");
            context.register(DemoDataSeeder.class);
            context.refresh();
            assertThat(context.getBeanNamesForType(DemoDataSeeder.class)).isEmpty();
        }
    }
}

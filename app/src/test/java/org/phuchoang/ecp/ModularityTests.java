package org.phuchoang.ecp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    @BeforeAll
    static void detectOnlyDeclaredBoundedContexts() {
        System.setProperty("spring.modulith.detection-strategy", "explicitly-annotated");
    }

    @Test
    void isValid() {
        ApplicationModules.of(EcpApplication.class).verify();
    }
}

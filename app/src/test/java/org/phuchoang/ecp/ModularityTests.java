package org.phuchoang.ecp;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    @Test
    void isValid() {
        ApplicationModules.of(EcpApplication.class).verify();
    }
}

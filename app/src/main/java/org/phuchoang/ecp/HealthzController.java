package org.phuchoang.ecp;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HealthzController {

    @GetMapping("/healthz")
    String healthz() {
        return "OK";
    }
}

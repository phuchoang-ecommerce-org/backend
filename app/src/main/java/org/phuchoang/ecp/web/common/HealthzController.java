package org.phuchoang.ecp.web.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lightweight liveness endpoint for process and platform probes; it intentionally has no dependencies. */
@RestController
class HealthzController {

    @GetMapping("/healthz")
    String healthz() {
        return "OK";
    }
}

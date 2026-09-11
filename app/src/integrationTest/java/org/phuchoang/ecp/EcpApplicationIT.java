package org.phuchoang.ecp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * The full-context smoke test formerly in {@code test} as {@code EcpApplicationTests}
 * ("contextLoads"), moved here once Flyway became real (EN-DATA-2): the full context now wires a
 * {@code DataSource} and runs every migration at startup, which needs a real PostgreSQL — the
 * fast suite must not (ArchUnit's {@code fastSuiteImportsNoTestcontainers} rule).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class EcpApplicationIT {

    @Test
    void contextLoads() {
    }
}

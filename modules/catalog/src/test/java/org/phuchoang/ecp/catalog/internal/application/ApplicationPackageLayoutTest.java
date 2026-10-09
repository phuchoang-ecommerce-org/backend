package org.phuchoang.ecp.catalog.internal.application;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationPackageLayoutTest {

    private static final Path APPLICATION_SOURCES = Path.of("src", "main", "java", "org", "phuchoang", "ecp",
        "catalog", "internal", "application");

    @Test
    void capabilityPackagesPlaceTheirCqrsRoleImmediatelyBelowTheCapability() throws IOException {
        List<String> sourcePaths;
        try (var paths = Files.walk(APPLICATION_SOURCES)) {
            sourcePaths = paths.filter(path -> path.toString().endsWith(".java"))
                .map(APPLICATION_SOURCES::relativize)
                .map(Path::toString)
                .map(path -> path.replace('\\', '/'))
                .toList();
        }

        assertThat(sourcePaths)
            .allMatch(path -> !path.startsWith("administration/") || path.startsWith("administration/command/"))
            .allMatch(path -> !path.startsWith("browse/") || path.startsWith("browse/query/"))
            .allMatch(path -> !path.startsWith("search/") || path.startsWith("search/query/")
                || path.startsWith("search/projection/"));
    }
}

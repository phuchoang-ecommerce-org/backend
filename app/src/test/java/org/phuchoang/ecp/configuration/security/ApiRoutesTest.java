package org.phuchoang.ecp.configuration.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiRoutesTest {

    @Test
    void exposesEveryGuestCatalogRouteFamily() {
        assertThat(ApiRoutes.PUBLIC_GET).contains(
            "/api/v1/categories",
            "/api/v1/categories/*",
            "/api/v1/categories/*/products",
            "/api/v1/products/*",
            "/api/v1/products/*/rating-summary",
            "/api/v1/products/*/variants",
            "/api/v1/products/*/variants/*");
    }
}

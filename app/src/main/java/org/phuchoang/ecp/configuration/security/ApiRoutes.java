package org.phuchoang.ecp.configuration.security;

/**
 * The coarse HTTP route groups {@link SecurityConfig} reasons about — which endpoints are reachable
 * without a bearer token. Fine-grained "may this caller do this" decisions never live here; they
 * are the bounded contexts' application-layer authorization calls.
 */
final class ApiRoutes {

    /** Liveness and platform probes. */
    static final String[] OPERATIONAL_GET = { "/healthz" };

    /** Anonymous identity flows: registration, verification, login, renewal and password recovery. */
    static final String[] PUBLIC_POST = {
        "/api/v1/accounts",
        "/api/v1/account-verifications",
        "/api/v1/account-verification-requests",
        "/api/v1/sessions",
        "/api/v1/session-renewals",
        "/api/v1/password-reset-requests",
        "/api/v1/password-resets"
    };

    /** Guest-readable catalog surfaces (`BR-CAT-02`: only published products are ever returned). */
    static final String[] PUBLIC_GET = {
        "/api/v1/products/*",
        "/api/v1/products/*/rating-summary",
        "/api/v1/products/*/variants",
        "/api/v1/products/*/variants/*"
    };

    private ApiRoutes() {
    }
}

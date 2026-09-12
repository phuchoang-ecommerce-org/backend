package org.phuchoang.ecp.identity.application.mapper;

/**
 * `04-shared/OpenAPI/components/schemas/identity.yaml#/Session`, minus the cookie half of that
 * schema (`ADR-0025`'s cookie is a Next.js-server concern outside this repo — see Sprint 03 plan's
 * "Key design decisions"). {@code refreshToken} is always present here; whether the web layer
 * puts it in a JSON body or a cookie is its own decision, not this port's.
 */
public record LoginResult(String accessToken, String refreshToken, long expiresInSeconds, boolean restricted,
        AccountSummary account) {
}

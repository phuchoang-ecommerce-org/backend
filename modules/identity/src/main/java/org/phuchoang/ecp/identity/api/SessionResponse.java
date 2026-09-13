package org.phuchoang.ecp.identity.api;

/**
 * `components/schemas/identity.yaml#/Session`, minus the cookie half — see the Sprint 03 plan's
 * "Key design decisions": this backend is always a JSON API, never a cookie-issuing one.
 */
public record SessionResponse(String accessToken, String refreshToken, long expiresIn, boolean restricted,
        AccountView account) {
}

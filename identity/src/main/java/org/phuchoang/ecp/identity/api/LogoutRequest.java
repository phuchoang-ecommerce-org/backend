package org.phuchoang.ecp.identity.api;

/**
 * `logOut` — a scope-decision addition to the original contract (see the Sprint 03 plan's "Key
 * design decisions" and Review Notes): this backend has no cookie, so `DELETE /sessions/current`
 * needs some way to name the refresh token to revoke. {@code refreshToken} is optional — its
 * absence is treated as `UC-CUS-04` E1 (already ended), never an error.
 */
public record LogoutRequest(String refreshToken) {
}

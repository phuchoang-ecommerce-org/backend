package org.phuchoang.ecp.identity.internal.application.port;

import org.phuchoang.ecp.identity.internal.domain.model.RoleCode;

import java.util.Set;
import java.util.UUID;

/**
 * Mints the short-lived, stateless access token (`ADR-0016` §4). Verified without a datastore
 * lookup — signing/verification config (RS256, this sprint's scope decision; see Sprint 03 Review
 * Notes) lives in {@code app}'s `security` package, wired in through this port.
 */
public interface AccessTokenIssuer {

    IssuedAccessToken issue(UUID accountId, Set<RoleCode> roles);

    record IssuedAccessToken(String token, long expiresInSeconds) {
    }
}

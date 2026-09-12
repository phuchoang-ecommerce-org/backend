package org.phuchoang.ecp.identity.application;

import org.phuchoang.ecp.identity.domain.RoleCode;

import java.util.Set;
import java.util.UUID;

/**
 * The acting party for an application-layer command — accountId plus the roles carried by the
 * access token at the moment of the call (`ADR-0016` §5: roles are re-read at every refresh, not
 * looked up live, so this reflects the token, not necessarily the account's current roles).
 * {@code accountId} is {@code null} for an unauthenticated (`GUEST`) caller.
 */
public record CallerContext(UUID accountId, Set<RoleCode> roles) {

    public static final CallerContext GUEST = new CallerContext(null, Set.of(RoleCode.GUEST));

    /** Converts the api layer's plain role-name strings (`identity.api.Actor`) into this module's domain enum. */
    public static CallerContext of(UUID accountId, Set<String> roleNames) {
        Set<RoleCode> roles = roleNames.stream()
            .map(RoleCode::valueOf)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return new CallerContext(accountId, roles);
    }

    public boolean hasRole(RoleCode role) {
        return roles.contains(role);
    }
}

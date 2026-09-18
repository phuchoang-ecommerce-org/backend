package org.phuchoang.ecp.identity.internal.application.security;

import org.phuchoang.ecp.identity.internal.domain.model.RoleCode;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The acting party for an application-layer command — accountId plus the roles carried by the
 * access token at the moment of the call (`ADR-0016` §5: roles are re-read at every refresh, not
 * looked up live, so this reflects the token, not necessarily the account's current roles).
 * {@code accountId} is {@code null} for an unauthenticated (`GUEST`) caller.
 *
 * <p>This is the <em>internal</em> authorization context; the external representation is
 * {@code identity.api.authorization.IdentityActor}, converted into this type once at the module
 * boundary and never again deeper inside the application layer.
 */
public record CallerContext(UUID accountId, Set<RoleCode> roles) {

    public static final CallerContext GUEST = new CallerContext(null, Set.of(RoleCode.GUEST));

    /** Converts the api layer's plain role-name strings into this module's domain enum. */
    public static CallerContext of(UUID accountId, Set<String> roleNames) {
        Set<RoleCode> roles = roleNames.stream()
            .map(RoleCode::valueOf)
            .collect(Collectors.toUnmodifiableSet());
        return new CallerContext(accountId, roles);
    }

    public boolean hasRole(RoleCode role) {
        return roles.contains(role);
    }
}

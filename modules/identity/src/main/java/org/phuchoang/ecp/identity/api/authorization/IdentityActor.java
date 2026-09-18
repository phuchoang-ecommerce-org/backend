package org.phuchoang.ecp.identity.api.authorization;

import java.util.Set;
import java.util.UUID;

/**
 * The authenticated caller as every consumer of {@code identity.api} sees it — the web layer and
 * other modules' application layers alike: accountId plus the role names carried by the access
 * token. {@code null} {@code accountId} with {@code roles = {"GUEST"}} is an unauthenticated
 * caller.
 *
 * <p>Deliberately plain data (no domain enum) so this type crosses the module boundary without
 * pulling {@code identity.internal.domain} along with it
 * (`apiDoesNotDependOnDomainOrInfrastructureInternals`). It is converted exactly once, at the
 * module boundary, into the internal {@code CallerContext}.
 */
public record IdentityActor(UUID accountId, Set<String> roles) {

    public static final IdentityActor GUEST = new IdentityActor(null, Set.of("GUEST"));
}

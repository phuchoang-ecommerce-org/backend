package org.phuchoang.ecp.identity.api.authorization;

import java.util.Set;
import java.util.UUID;

/**
 * The authenticated caller, as the web layer (or, once other modules exist, their application
 * layer) sees it — accountId plus the role names carried by the access token. {@code null}
 * {@code accountId} with {@code roles = {"GUEST"}} represents an unauthenticated caller.
 *
 * <p>Deliberately plain data (no domain enum) so this type crosses the module boundary without
 * pulling {@code identity.domain} along with it (`apiDoesNotDependOnDomainOrInfrastructureInternals`).
 */
public record Actor(UUID accountId, Set<String> roles) {

    public static final Actor GUEST = new Actor(null, Set.of("GUEST"));
}

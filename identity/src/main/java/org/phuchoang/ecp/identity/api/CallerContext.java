package org.phuchoang.ecp.identity.api;

import java.util.Set;
import java.util.UUID;

/**
 * The acting party, as seen by a caller of {@link AuthorizationService} from outside this module
 * — accountId plus role names, mirroring {@link Actor} for exactly the same reason: this type
 * must cross the module boundary without pulling {@code identity.domain}'s {@code RoleCode} along
 * with it (`apiDoesNotDependOnDomainOrInfrastructureInternals`). {@code identity}'s own
 * application services keep using the internal {@code identity.application.CallerContext}; this
 * is the wire-shaped counterpart other modules' application layers use.
 */
public record CallerContext(UUID accountId, Set<String> roleNames) {

    public static final CallerContext GUEST = new CallerContext(null, Set.of("GUEST"));
}

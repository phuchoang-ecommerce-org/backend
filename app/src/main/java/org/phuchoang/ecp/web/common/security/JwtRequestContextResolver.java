package org.phuchoang.ecp.web.common.security;

import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.web.common.filter.CorrelationIdFilter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * {@link RequestContextResolver} over Spring Security's {@link Jwt}: {@code sub} is the account
 * id, the {@code roles} claim the role names (absent → no roles rather than a crash). The
 * correlation id is the one {@link CorrelationIdFilter} established for this request; a header
 * value that is not a UUID is echoed by the filter but not propagated into commands as one.
 */
@Component
public class JwtRequestContextResolver implements RequestContextResolver {

    static final String ROLES_CLAIM = "roles";

    @Override
    public RequestContext resolve(Jwt jwt) {
        return new RequestContext(currentCorrelationId(), callerOf(jwt));
    }

    private static IdentityActor callerOf(Jwt jwt) {
        if (jwt == null) {
            return IdentityActor.GUEST;
        }
        List<String> roles = jwt.getClaimAsStringList(ROLES_CLAIM);
        return new IdentityActor(UUID.fromString(jwt.getSubject()), roles == null ? Set.of() : Set.copyOf(roles));
    }

    private static UUID currentCorrelationId() {
        String value = CorrelationIdFilter.currentCorrelationId();
        if (value != null) {
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException notAUuid) {
                // fall through: the filter echoes the raw header, commands get a fresh UUID
            }
        }
        return UUID.randomUUID();
    }
}

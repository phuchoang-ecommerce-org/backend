package org.phuchoang.ecp.web.common.security;

import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Supplies an all-access local actor when authentication is disabled. The combined roles make the
 * actor pass every current permission-matrix row, while the configurable account id lets manual
 * tests target an existing account for {@code /me} and ownership-based operations.
 */
@Component
@Profile("no-auth & !prod")
public class NoAuthenticationRequestContextResolver implements RequestContextResolver {

    static final Set<String> ALL_ROLES = Set.of(
        "GUEST",
        "CUSTOMER",
        "STAFF",
        "WAREHOUSE_OPERATOR",
        "CUSTOMER_SUPPORT",
        "ADMINISTRATOR"
    );

    private final IdentityActor localActor;

    public NoAuthenticationRequestContextResolver(
            @Value("${ecp.security.no-auth.account-id:10000000-0000-0000-0000-000000000001}") UUID accountId) {
        this.localActor = new IdentityActor(accountId, ALL_ROLES);
    }

    @Override
    public RequestContext resolve(Jwt ignored) {
        return new RequestContext(JwtRequestContextResolver.currentCorrelationId(), localActor);
    }
}

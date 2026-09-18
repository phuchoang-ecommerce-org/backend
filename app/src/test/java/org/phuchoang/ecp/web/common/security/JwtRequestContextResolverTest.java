package org.phuchoang.ecp.web.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.web.common.filter.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtRequestContextResolverTest {

    private final JwtRequestContextResolver resolver = new JwtRequestContextResolver();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void translatesSubjectAndRolesClaimIntoTheIdentityActor() {
        UUID accountId = UUID.randomUUID();
        Jwt jwt = jwt(accountId, List.of("CUSTOMER", "STAFF"));

        RequestContext context = resolver.resolve(jwt);

        assertThat(context.caller()).isEqualTo(new IdentityActor(accountId, Set.of("CUSTOMER", "STAFF")));
    }

    @Test
    void anAbsentPrincipalIsAGuestAndAMissingRolesClaimMeansNoRoles() {
        assertThat(resolver.resolve(null).caller()).isEqualTo(IdentityActor.GUEST);
        assertThat(resolver.resolve(jwt(UUID.randomUUID(), null)).caller().roles()).isEmpty();
    }

    @Test
    void usesTheFiltersCorrelationIdWhenItIsAUuidAndMintsOneOtherwise() {
        UUID correlationId = UUID.randomUUID();
        MDC.put(CorrelationIdFilter.MDC_KEY, correlationId.toString());
        assertThat(resolver.resolve(null).correlationId()).isEqualTo(correlationId);

        MDC.put(CorrelationIdFilter.MDC_KEY, "trace-abc");
        assertThat(resolver.resolve(null).correlationId()).isNotNull();

        MDC.clear();
        assertThat(resolver.resolve(null).correlationId()).isNotNull();
    }

    private static Jwt jwt(UUID subject, List<String> roles) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "RS256").subject(subject.toString())
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
        if (roles != null) {
            builder.claim(JwtRequestContextResolver.ROLES_CLAIM, roles);
        }
        return builder.build();
    }
}

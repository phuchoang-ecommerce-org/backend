package org.phuchoang.ecp.identity.internal.infrastructure.security;

import org.phuchoang.ecp.identity.internal.application.port.AccessTokenIssuer;
import org.phuchoang.ecp.identity.internal.domain.model.RoleCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * `ADR-0016` §4's stateless access token. Signing key material and the {@code JwtEncoder} bean
 * are configured in {@code app}'s {@code security} package; this adapter only shapes the claims
 * (`sub`, `roles`, `iat`, `exp`, `jti`, `iss` — `Security.md` §4.2, nothing else, `NFR-SEC-07`).
 *
 * <p><b>Scope decision</b> (Sprint 03 Review Notes): signed with RS256, not the `[ASSUMPTION]`
 * EdDSA in Security.md §4.2 — RS256 is that document's own named fallback and what Spring
 * Security's {@code NimbusJwtEncoder} supports without extra key-format handling.
 */
@Component
class JwtAccessTokenIssuer implements AccessTokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final long accessTokenTtlSeconds;
    private final Clock clock;

    JwtAccessTokenIssuer(JwtEncoder jwtEncoder,
            @Value("${ecp.jwt.issuer:ecp-api}") String issuer,
            @Value("${ecp.jwt.access-token-ttl-seconds:900}") long accessTokenTtlSeconds,
            Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
        this.clock = clock;
    }

    @Override
    public IssuedAccessToken issue(UUID accountId, Set<RoleCode> roles) {
        Instant now = Instant.now(clock);
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .issuedAt(now)
            .expiresAt(now.plusSeconds(accessTokenTtlSeconds))
            .subject(accountId.toString())
            .id(UUID.randomUUID().toString())
            .claim("roles", roles.stream().map(RoleCode::name).collect(Collectors.toUnmodifiableSet()))
            .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedAccessToken(token, accessTokenTtlSeconds);
    }
}

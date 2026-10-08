package org.phuchoang.ecp.identity.internal.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Signing-claim settings owned by Identity's JWT issuance adapter. */
@ConfigurationProperties("ecp.jwt")
public record IdentityTokenProperties(@DefaultValue("ecp-api") String issuer,
        @DefaultValue("900") long accessTokenTtlSeconds) {

    public IdentityTokenProperties {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("ecp.jwt.issuer must not be blank.");
        }
        if (accessTokenTtlSeconds < 1) {
            throw new IllegalArgumentException("ecp.jwt.access-token-ttl-seconds must be at least 1.");
        }
    }
}

package org.phuchoang.ecp.configuration.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code ecp.jwt.*} key material. {@code generateDevKeys} is the explicit opt-in for an in-memory
 * keypair when no files are configured; the {@code prod} profile turns it off so a production-like
 * environment fails at startup instead of silently minting tokens that die with the process.
 * ({@code issuer} and {@code access-token-ttl-seconds} under the same prefix belong to the
 * identity module's token issuer, not to this record.)
 */
@ConfigurationProperties("ecp.jwt")
public record JwtProperties(@DefaultValue("") String privateKeyPath, @DefaultValue("") String publicKeyPath,
        @DefaultValue("true") boolean generateDevKeys) {

    public boolean hasKeyFiles() {
        return !privateKeyPath.isBlank() && !publicKeyPath.isBlank();
    }
}

package org.phuchoang.ecp.identity.internal.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Argon2id work factors for Identity's password-hashing adapter. */
@ConfigurationProperties("ecp.password")
public record PasswordHashingProperties(@DefaultValue("16") int argon2SaltLength,
        @DefaultValue("32") int argon2HashLength, @DefaultValue("1") int argon2Parallelism,
        @DefaultValue("19456") int argon2MemoryKib, @DefaultValue("2") int argon2Iterations) {

    public PasswordHashingProperties {
        if (argon2SaltLength < 1 || argon2HashLength < 1 || argon2Parallelism < 1 || argon2MemoryKib < 1
                || argon2Iterations < 1) {
            throw new IllegalArgumentException("All ecp.password Argon2 parameters must be positive.");
        }
    }
}

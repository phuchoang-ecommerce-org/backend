package org.phuchoang.ecp.identity.internal.infrastructure.security;

import org.phuchoang.ecp.identity.internal.application.port.PasswordEncoder;
import org.phuchoang.ecp.identity.internal.infrastructure.configuration.PasswordHashingProperties;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * `[ASSUMPTION]` Security.md §4.5 — Argon2id, memory-hard, parameters re-tunable without a schema
 * change (`ecp.password.*`, `application.yml`). The plaintext never appears in a log anywhere
 * behind this adapter (`NFR-SEC-07`).
 */
@Component
class Argon2PasswordEncoderAdapter implements PasswordEncoder {

    private final Argon2PasswordEncoder delegate;

    Argon2PasswordEncoderAdapter(PasswordHashingProperties properties) {
        this.delegate = new Argon2PasswordEncoder(properties.argon2SaltLength(), properties.argon2HashLength(),
            properties.argon2Parallelism(), properties.argon2MemoryKib(), properties.argon2Iterations());
    }

    @Override
    public String encode(String rawPassword) {
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedHash) {
        return delegate.matches(rawPassword, encodedHash);
    }
}

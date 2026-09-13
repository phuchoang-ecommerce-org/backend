package org.phuchoang.ecp.identity.infrastructure.security;

import org.phuchoang.ecp.identity.application.port.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
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

    Argon2PasswordEncoderAdapter(
            @Value("${ecp.password.argon2-salt-length:16}") int saltLength,
            @Value("${ecp.password.argon2-hash-length:32}") int hashLength,
            @Value("${ecp.password.argon2-parallelism:1}") int parallelism,
            @Value("${ecp.password.argon2-memory-kib:19456}") int memoryKib,
            @Value("${ecp.password.argon2-iterations:2}") int iterations) {
        this.delegate = new Argon2PasswordEncoder(saltLength, hashLength, parallelism, memoryKib, iterations);
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

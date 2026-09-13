package org.phuchoang.ecp.identity.domain;

import java.util.Objects;

/**
 * A one-way, salted, computationally adaptive password hash (`NFR-SEC-02`). This value object
 * never carries a plaintext password — hashing happens in the {@code PasswordEncoder} port, in
 * application/infrastructure, never here (`NFR-SEC-07`: the plaintext never reaches the domain).
 */
public final class CredentialHash {

    private final String value;

    public CredentialHash(String hashedValue) {
        this.value = Objects.requireNonNull(hashedValue, "credential hash must not be null");
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return "CredentialHash[REDACTED]";
    }
}

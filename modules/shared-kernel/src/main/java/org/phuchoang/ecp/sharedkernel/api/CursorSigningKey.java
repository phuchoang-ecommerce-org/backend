package org.phuchoang.ecp.sharedkernel.api;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/** HMAC key material identified by a non-secret key ID. */
public final class CursorSigningKey {

    private final String id;
    private final byte[] secret;

    public CursorSigningKey(String id, byte[] secret) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Cursor key ID must not be blank.");
        }
        this.id = id;
        this.secret = Arrays.copyOf(Objects.requireNonNull(secret, "secret"), secret.length);
        if (this.secret.length < 32) {
            throw new IllegalArgumentException("Cursor HMAC secrets must be at least 32 bytes.");
        }
    }

    public String id() {
        return id;
    }

    byte[] secret() {
        return Arrays.copyOf(secret, secret.length);
    }

    /** Builds a key from an environment-supplied UTF-8 secret. */
    public static CursorSigningKey utf8(String id, String secret) {
        return new CursorSigningKey(id, Objects.requireNonNull(secret, "secret").getBytes(StandardCharsets.UTF_8));
    }
}

package org.phuchoang.ecp.identity.internal.application.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Opaque refresh-token values. The value handed to the caller is never stored — only its SHA-256
 * hash is (`identity_token.token_hash`), so a database read does not yield a usable token
 * (Security.md §4.5). Pure JDK — no external hashing library needed for a fixed-length,
 * non-password digest.
 */
public final class RefreshTokenGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private RefreshTokenGenerator() {
    }

    public static String generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is a required JDK algorithm", e);
        }
    }
}

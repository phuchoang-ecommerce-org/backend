package org.phuchoang.ecp.configuration.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

/**
 * Generates a fresh in-memory RSA keypair for this process only — tokens stop verifying the
 * moment it restarts. Selected only when {@code ecp.jwt.generate-dev-keys} is on.
 */
final class DevelopmentJwtKeyProvider implements JwtKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(DevelopmentJwtKeyProvider.class);

    @Override
    public KeyPair load() {
        log.warn("No ecp.jwt.private-key-path/public-key-path configured — generating an in-memory RSA "
            + "keypair for this process only (ecp.jwt.generate-dev-keys=true). Tokens will stop verifying on "
            + "restart. Never do this outside local development.");
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA is a required JDK algorithm", e);
        }
    }
}

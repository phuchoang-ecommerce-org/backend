package org.phuchoang.ecp.security;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

/**
 * The RS256 signing keypair (`ADR-0016` §4 — see Sprint 03 Review Notes for the RS256-not-EdDSA
 * scope decision). When no key path is configured a fresh in-memory keypair is generated at
 * startup — clearly logged, and never acceptable outside local development, since tokens signed
 * with it stop verifying the moment the process restarts.
 */
@Configuration
public class JwtKeysConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtKeysConfig.class);

    @Bean
    RSAKey rsaJwk(
            @Value("${ecp.jwt.private-key-path:}") String privateKeyPath,
            @Value("${ecp.jwt.public-key-path:}") String publicKeyPath) {
        KeyPair keyPair = (privateKeyPath.isBlank() || publicKeyPath.isBlank())
            ? generateDevKeyPair()
            : loadKeyPair(privateKeyPath, publicKeyPath);
        return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
            .privateKey((RSAPrivateKey) keyPair.getPrivate())
            .keyID(UUID.randomUUID().toString())
            .build();
    }

    @Bean
    JwtEncoder jwtEncoder(RSAKey rsaJwk) {
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new com.nimbusds.jose.jwk.JWKSet(rsaJwk));
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    JwtDecoder jwtDecoder(RSAKey rsaJwk) throws com.nimbusds.jose.JOSEException {
        return NimbusJwtDecoder.withPublicKey(rsaJwk.toRSAPublicKey()).build();
    }

    private KeyPair generateDevKeyPair() {
        log.warn("No ecp.jwt.private-key-path/public-key-path configured — generating an in-memory RSA "
            + "keypair for this process only. Tokens will stop verifying on restart. Never do this outside "
            + "local development.");
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA is a required JDK algorithm", e);
        }
    }

    private KeyPair loadKeyPair(String privateKeyPath, String publicKeyPath) {
        try {
            byte[] privateBytes = decodePem(Files.readString(Path.of(privateKeyPath)));
            byte[] publicBytes = decodePem(Files.readString(Path.of(publicKeyPath)));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPrivateKey privateKey =
                (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateBytes));
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(publicBytes));
            return new KeyPair(publicKey, privateKey);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read JWT key material", e);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Configured JWT key material is not a valid RSA PKCS8/X509 pair", e);
        }
    }

    private byte[] decodePem(String pem) {
        String stripped = pem.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(stripped);
    }
}

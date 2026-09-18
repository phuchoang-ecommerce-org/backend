package org.phuchoang.ecp.configuration.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * The RS256 signing keypair (`ADR-0016` §4) and the Spring Security encoder/decoder over it.
 * Environment policy — file keys, development keys, or fail fast — is decided by
 * {@link #keyProvider}; the cryptographic wiring below never changes with it.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtKeysConfig {

    @Bean
    RSAKey rsaJwk(JwtProperties properties) {
        KeyPair keyPair = keyProvider(properties).load();
        return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
            .privateKey((RSAPrivateKey) keyPair.getPrivate())
            .keyID(UUID.randomUUID().toString())
            .build();
    }

    @Bean
    JwtEncoder jwtEncoder(RSAKey rsaJwk) {
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaJwk));
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    JwtDecoder jwtDecoder(RSAKey rsaJwk) throws JOSEException {
        return NimbusJwtDecoder.withPublicKey(rsaJwk.toRSAPublicKey()).build();
    }

    static JwtKeyProvider keyProvider(JwtProperties properties) {
        if (properties.hasKeyFiles()) {
            return new FileJwtKeyProvider(properties.privateKeyPath(), properties.publicKeyPath());
        }
        if (properties.generateDevKeys()) {
            return new DevelopmentJwtKeyProvider();
        }
        throw new IllegalStateException("JWT key material is required: set ecp.jwt.private-key-path and "
            + "ecp.jwt.public-key-path (ECP_JWT_PRIVATE_KEY_PATH / ECP_JWT_PUBLIC_KEY_PATH), or enable "
            + "ecp.jwt.generate-dev-keys for local development only.");
    }
}

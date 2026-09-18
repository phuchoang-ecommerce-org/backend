package org.phuchoang.ecp.configuration.security;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** Loads a PEM-encoded PKCS8 private key and X.509 public key from the configured paths. */
final class FileJwtKeyProvider implements JwtKeyProvider {

    private final Path privateKeyPath;
    private final Path publicKeyPath;

    FileJwtKeyProvider(String privateKeyPath, String publicKeyPath) {
        this.privateKeyPath = Path.of(privateKeyPath);
        this.publicKeyPath = Path.of(publicKeyPath);
    }

    @Override
    public KeyPair load() {
        try {
            byte[] privateBytes = decodePem(Files.readString(privateKeyPath));
            byte[] publicBytes = decodePem(Files.readString(publicKeyPath));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateBytes));
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(publicBytes));
            return new KeyPair(publicKey, privateKey);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read JWT key material", e);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Configured JWT key material is not a valid RSA PKCS8/X509 pair", e);
        }
    }

    private static byte[] decodePem(String pem) {
        String stripped = pem.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(stripped);
    }
}

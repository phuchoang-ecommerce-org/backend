package org.phuchoang.ecp.messaging.revalidation;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** {@code sha256=<hex(HMAC-SHA256(secret, body))>} over the exact bytes forwarded. */
@Component
class HmacSha256PayloadSigner implements PayloadSigner {

    private static final String ALGORITHM = "HmacSHA256";

    private final RevalidationProperties properties;

    HmacSha256PayloadSigner(RevalidationProperties properties) {
        this.properties = properties;
    }

    @Override
    public String sign(String payload) {
        return sign(properties.secret(), payload);
    }

    static String sign(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("HMAC-SHA256 signing failed", exception);
        }
    }
}

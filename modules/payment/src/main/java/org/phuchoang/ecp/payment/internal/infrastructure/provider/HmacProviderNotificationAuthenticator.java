package org.phuchoang.ecp.payment.internal.infrastructure.provider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.phuchoang.ecp.payment.internal.application.callback.command.ProviderNotification;
import org.phuchoang.ecp.payment.internal.application.callback.command.ProviderNotificationAuthenticator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Adapter-level HMAC verification. An absent production secret rejects every callback. */
@Component
class HmacProviderNotificationAuthenticator implements ProviderNotificationAuthenticator {
    private final String secret;
    HmacProviderNotificationAuthenticator(@Value("${ecp.payment.provider-webhook-secret:}") String secret) { this.secret = secret; }
    @Override public boolean authentic(ProviderNotification notification, String signature) {
        if (secret.isBlank() || signature == null || notification == null) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal((notification.providerReference() + "|" + notification.outcome() + "|" + notification.amount() + "|" + notification.providerEventId()).getBytes(StandardCharsets.UTF_8));
            return MessageDigest.isEqual(expected, java.util.HexFormat.of().parseHex(signature));
        } catch (Exception ignored) { return false; }
    }
}

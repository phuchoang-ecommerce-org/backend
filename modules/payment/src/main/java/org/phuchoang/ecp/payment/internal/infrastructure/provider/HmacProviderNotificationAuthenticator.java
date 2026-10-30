package org.phuchoang.ecp.payment.internal.infrastructure.provider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
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
            byte[] expected = sign(notification, secret);
            return MessageDigest.isEqual(expected, java.util.HexFormat.of().parseHex(signature));
        } catch (Exception ignored) { return false; }
    }

    static String signature(ProviderNotification notification, String secret) {
        return java.util.HexFormat.of().formatHex(sign(notification, secret));
    }

    private static byte[] sign(ProviderNotification notification, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(canonical(notification).getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) { throw new IllegalStateException("HMAC is unavailable", exception); }
    }

    /** Every field the callback carries is bound before any persistence occurs. */
    private static String canonical(ProviderNotification notification) {
        return String.join("|", text(notification.providerReference()), text(notification.outcome()),
            notification.amount() == null ? "" : notification.amount().toPlainString(), text(notification.currency()),
            text(notification.providerEventId()), instant(notification.occurredAt()), payload(notification.providerPayload()));
    }

    private static String text(String value) { return value == null ? "" : value; }
    private static String instant(Instant value) { return value == null ? "" : value.toString(); }
    private static String payload(Map<String, Object> value) {
        return value == null ? "" : canonicalValue(value);
    }

    /** Minimal deterministic JSON form so map order can never change a signature. */
    private static String canonicalValue(Object value) {
        if (value == null) return "null";
        if (value instanceof Map<?, ?> map) {
            var entries = new ArrayList<Map.Entry<String, Object>>();
            for (var entry : map.entrySet()) entries.add(Map.entry(String.valueOf(entry.getKey()), entry.getValue()));
            entries.sort(Comparator.comparing(Map.Entry::getKey));
            return "{" + entries.stream().map(entry -> quote(entry.getKey()) + ":" + canonicalValue(entry.getValue()))
                .collect(java.util.stream.Collectors.joining(",")) + "}";
        }
        if (value instanceof Iterable<?> values) {
            var items = new ArrayList<String>();
            for (Object item : values) items.add(canonicalValue(item));
            return "[" + String.join(",", items) + "]";
        }
        if (value.getClass().isArray()) {
            var items = new ArrayList<String>();
            int length = java.lang.reflect.Array.getLength(value);
            for (int index = 0; index < length; index++) items.add(canonicalValue(java.lang.reflect.Array.get(value, index)));
            return "[" + String.join(",", items) + "]";
        }
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        return quote(value.toString());
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }
}

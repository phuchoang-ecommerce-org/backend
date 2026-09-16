package org.phuchoang.ecp.sharedkernel.api.cursor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/** Framework-free HMAC-SHA-256 implementation of the common cursor contract. */
public final class HmacCursorCodec implements CursorCodec {

    private static final String VERSION = "v1";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final CursorSigningKey activeKey;
    private final CursorSigningKey previousKey;

    public HmacCursorCodec(CursorSigningKey activeKey, CursorSigningKey previousKey) {
        this.activeKey = Objects.requireNonNull(activeKey, "activeKey");
        if (previousKey != null && activeKey.id().equals(previousKey.id())) {
            throw new IllegalArgumentException("Active and previous cursor key IDs must differ.");
        }
        this.previousKey = previousKey;
    }

    @Override
    public String encode(CursorContext context, List<CursorValue> sortValues, UUID tieBreaker) {
        CursorPosition position = new CursorPosition(sortValues, tieBreaker);
        String values = position.sortValues().stream().map(HmacCursorCodec::encodeValue).collect(Collectors.joining(","));
        String payload = String.join(".", VERSION, encodeText(activeKey.id()), context.fingerprint(), values,
            position.tieBreaker().toString());
        return encodeText(payload) + "." + ENCODER.encodeToString(sign(payload, activeKey));
    }

    @Override
    public CursorPosition decode(String token, CursorContext expectedContext) {
        try {
            if (token == null || token.isBlank()) {
                throw new InvalidCursorException();
            }
            String[] tokenParts = token.split("\\.", -1);
            if (tokenParts.length != 2) {
                throw new InvalidCursorException();
            }
            String payload = decodeText(tokenParts[0]);
            byte[] signature = DECODER.decode(tokenParts[1]);
            String[] fields = payload.split("\\.", -1);
            if (fields.length != 5 || !VERSION.equals(fields[0])) {
                throw new InvalidCursorException();
            }
            CursorSigningKey key = verificationKey(decodeText(fields[1]));
            if (!MessageDigest.isEqual(signature, sign(payload, key))
                    || !MessageDigest.isEqual(fields[2].getBytes(StandardCharsets.US_ASCII),
                        expectedContext.fingerprint().getBytes(StandardCharsets.US_ASCII))) {
                throw new InvalidCursorException();
            }
            List<CursorValue> values = List.of(fields[3].split(",", -1)).stream()
                .map(HmacCursorCodec::decodeValue).toList();
            return new CursorPosition(values, UUID.fromString(fields[4]));
        } catch (InvalidCursorException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new InvalidCursorException();
        }
    }

    private CursorSigningKey verificationKey(String keyId) {
        if (activeKey.id().equals(keyId)) {
            return activeKey;
        }
        if (previousKey != null && previousKey.id().equals(keyId)) {
            return previousKey;
        }
        throw new InvalidCursorException();
    }

    private static String encodeValue(CursorValue value) {
        return value.type().name() + ":" + (value.value() == null ? "" : encodeText(value.value()));
    }

    private static CursorValue decodeValue(String encoded) {
        int separator = encoded.indexOf(':');
        if (separator < 1 || separator != encoded.lastIndexOf(':')) {
            throw new InvalidCursorException();
        }
        CursorValue.Type type = CursorValue.Type.valueOf(encoded.substring(0, separator));
        String raw = encoded.substring(separator + 1);
        CursorValue value = type == CursorValue.Type.NULL
            ? raw.isEmpty() ? CursorValue.nullValue() : invalidValue()
            : new CursorValue(type, decodeText(raw));
        validate(value);
        return value;
    }

    private static CursorValue invalidValue() {
        throw new InvalidCursorException();
    }

    private static void validate(CursorValue value) {
        try {
            switch (value.type()) {
                case DECIMAL -> new BigDecimal(value.value());
                case INSTANT -> Instant.parse(value.value());
                case INTEGER -> Integer.parseInt(value.value());
                case TEXT, NULL -> { }
            }
        } catch (RuntimeException exception) {
            throw new InvalidCursorException();
        }
    }

    private static String encodeText(String value) {
        return ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeText(String value) {
        return new String(DECODER.decode(value), StandardCharsets.UTF_8);
    }

    private static byte[] sign(String payload, CursorSigningKey key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.secret(), "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("HmacSHA256 is unavailable", exception);
        } catch (java.security.InvalidKeyException exception) {
            throw new IllegalStateException("Cursor signing key is invalid", exception);
        }
    }
}

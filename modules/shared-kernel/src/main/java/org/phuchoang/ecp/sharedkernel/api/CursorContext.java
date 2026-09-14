package org.phuchoang.ecp.sharedkernel.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * The canonical scope of a cursor: endpoint, resource, sort, and normalized filters.
 *
 * <p>The fingerprint is signed inside the token, so a token from a different account, category,
 * sort, or filter set cannot be replayed against this context.
 */
public record CursorContext(String endpointScope, String resourceScope, String sort,
                            Map<String, String> normalizedFilters) {

    public CursorContext {
        endpointScope = required(endpointScope, "endpointScope");
        resourceScope = required(resourceScope, "resourceScope");
        sort = required(sort, "sort");
        TreeMap<String, String> filters = new TreeMap<>();
        if (normalizedFilters != null) {
            normalizedFilters.forEach((key, value) -> filters.put(required(key, "filter name"),
                Objects.requireNonNull(value, "filter value")));
        }
        normalizedFilters = Collections.unmodifiableMap(filters);
    }

    /** Returns a stable SHA-256 fingerprint of every input that identifies an ordered result set. */
    public String fingerprint() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            append(digest, endpointScope);
            append(digest, resourceScope);
            append(digest, sort);
            for (Map.Entry<String, String> filter : normalizedFilters.entrySet()) {
                append(digest, filter.getKey());
                append(digest, filter.getValue());
            }
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return value;
    }

    private static void append(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(Integer.toString(bytes.length).getBytes(StandardCharsets.US_ASCII));
        digest.update((byte) ':');
        digest.update(bytes);
        digest.update((byte) ';');
    }
}

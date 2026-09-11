package org.phuchoang.ecp.web;

import java.util.Base64;
import java.nio.charset.StandardCharsets;

/**
 * Cursor pagination helpers (Integration Contract.md §3.1–§3.2). Cursor, never offset: a
 * {@code ?page=7} style parameter is never accepted anywhere in this API.
 */
public final class Pagination {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private Pagination() {
    }

    /** A request for more than {@link #MAX_SIZE} is clamped, never rejected (§3.1). */
    public static int clampSize(Integer requested) {
        if (requested == null || requested < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(requested, MAX_SIZE);
    }

    /**
     * Encodes an opaque cursor. The encoding is deliberately not part of the contract — a client
     * must treat the result as opaque and never construct or parse one (§3.1).
     */
    public static String encodeCursor(String raw) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static String decodeCursor(String cursor) {
        return new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
    }
}

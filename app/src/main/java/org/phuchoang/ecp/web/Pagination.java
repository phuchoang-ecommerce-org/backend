package org.phuchoang.ecp.web;


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

}

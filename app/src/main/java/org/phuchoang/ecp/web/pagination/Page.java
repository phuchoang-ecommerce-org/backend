package org.phuchoang.ecp.web.pagination;

/**
 * Pagination metadata (Integration Contract.md §3.1, {@code common.yaml#/Page}). {@code next} is
 * {@code null} when there are no further pages — a client never constructs or parses it.
 * {@code total} is {@code null} wherever counting is not cheap (Elasticsearch-backed collections).
 */
public record Page(int size, String next, Long total) {
}

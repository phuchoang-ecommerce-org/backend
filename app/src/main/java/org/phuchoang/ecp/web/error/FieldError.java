package org.phuchoang.ecp.web.error;

/**
 * One failing field in a validation failure (Integration Contract.md §4.3, {@code common.yaml#/FieldError}).
 * {@code field} is a dotted path with bracket indexing for array members — not an RFC 6901 JSON Pointer.
 */
public record FieldError(String field, String code, String detail) {
}

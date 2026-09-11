package org.phuchoang.ecp.web;

import java.util.List;

/**
 * RFC 9457 {@code application/problem+json} — one shape for every error this API returns
 * (Integration Contract.md §4.1, {@code common.yaml#/Problem}). Field order matches the schema.
 */
public record Problem(
    String type,
    String title,
    int status,
    String code,
    String detail,
    String instance,
    String correlationId,
    List<FieldError> errors) {

    private static final String TYPE_PREFIX = "https://ecp.example/errors/";

    public static Problem of(String code, String title, int status, String detail, String instance,
            String correlationId, List<FieldError> errors) {
        return new Problem(TYPE_PREFIX + code, title, status, code, detail, instance, correlationId, errors);
    }
}

package org.phuchoang.ecp.sharedkernel.api.error;

/**
 * The {@code GEN} domain — cross-cutting failures owned by no bounded context (Error Codes.md §3.1).
 * {@code ECP-GEN-5000} is the undeclared fallback an unmapped exception produces
 * (Backend Architecture.md §6.3) — it appears in no OpenAPI response schema, only here.
 *
 * <p>Two drifts recorded in Error Codes.md §5 — {@code ECP-SEC-4030}/{@code ECP-SEC-4290} and
 * {@code ECP-SCH-5030} in illustrative sequence-diagram prose — are resolved here by never having
 * a {@code SEC} or {@code SCH} entry: every handler in this codebase reaches this enum for the
 * cross-cutting cases, so the drift has nowhere to re-enter through code.
 */
public enum GenErrorCode implements ErrorCode {

    VALIDATION_FAILED("ECP-GEN-4000", 400, "Request validation failed"),
    NOT_AUTHENTICATED("ECP-GEN-4010", 401, "Not authenticated"),
    REFRESH_TOKEN_REJECTED("ECP-GEN-4011", 401, "Refresh token rejected; session chain invalidated"),
    FORBIDDEN("ECP-GEN-4030", 403, "Role does not permit this operation"),
    NOT_FOUND("ECP-GEN-4040", 404, "Not found"),
    RATE_LIMITED("ECP-GEN-4290", 429, "Rate limit exceeded"),
    UNMAPPED_ERROR("ECP-GEN-5000", 500, "An unexpected error occurred"),
    DEPENDENCY_UNAVAILABLE("ECP-GEN-5030", 503, "Dependency unavailable");

    private final String code;
    private final int httpStatus;
    private final String title;

    GenErrorCode(String code, int httpStatus, String title) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.title = title;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public int httpStatus() {
        return httpStatus;
    }

    @Override
    public String title() {
        return title;
    }
}

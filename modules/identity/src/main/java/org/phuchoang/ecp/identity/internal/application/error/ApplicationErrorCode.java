package org.phuchoang.ecp.identity.internal.application.error;

/** Error semantics owned by Identity application use cases, independent of API representation. */
public enum ApplicationErrorCode {
    VALIDATION_FAILED("ECP-GEN-4000"),
    NOT_AUTHENTICATED("ECP-GEN-4010"),
    REFRESH_TOKEN_REJECTED("ECP-GEN-4011"),
    FORBIDDEN("ECP-GEN-4030"),
    NOT_FOUND("ECP-GEN-4040");

    private final String code;

    ApplicationErrorCode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}

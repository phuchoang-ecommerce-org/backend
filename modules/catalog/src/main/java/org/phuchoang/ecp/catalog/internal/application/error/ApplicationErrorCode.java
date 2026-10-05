package org.phuchoang.ecp.catalog.internal.application.error;

/** Error semantics owned by Catalog application use cases, independent of API representation. */
public enum ApplicationErrorCode {
    VALIDATION_FAILED("ECP-GEN-4000"),
    NOT_FOUND("ECP-GEN-4040"),
    UNMAPPED_ERROR("ECP-GEN-5000"),
    DEPENDENCY_UNAVAILABLE("ECP-GEN-5030");

    private final String code;

    ApplicationErrorCode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}

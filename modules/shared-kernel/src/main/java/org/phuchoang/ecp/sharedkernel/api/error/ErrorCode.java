package org.phuchoang.ecp.sharedkernel.api.error;

/**
 * A machine-readable {@code ECP-<DOMAIN>-<NNNN>} code, per Integration Contract.md §4.2. One enum
 * per domain implements this in that domain's {@code api} package (Backend Architecture.md §6.3);
 * {@link GenErrorCode} is the {@code GEN} domain owned by no module. {@code httpStatus} is a plain
 * {@code int} rather than {@code org.springframework.http.HttpStatus} because shared-kernel carries
 * no Spring dependency (Module Dependency Diagram.md §7).
 */
public interface ErrorCode {

    String code();

    int httpStatus();

    String title();
}

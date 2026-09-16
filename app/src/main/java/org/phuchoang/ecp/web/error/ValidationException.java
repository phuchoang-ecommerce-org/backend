package org.phuchoang.ecp.web.error;

import java.util.List;

/**
 * Request validation failed — {@code ECP-GEN-4000}, with every failing field reported at once
 * (Integration Contract.md §4.3). Distinct from {@link org.phuchoang.ecp.sharedkernel.api.DomainException}
 * because a validation failure carries a field list, not a single {@code ErrorCode}, and the shape
 * is wire-format infrastructure rather than a shared-kernel concern (no module needs to throw it
 * from domain code — it is raised at the boundary, before a command exists).
 */
public class ValidationException extends RuntimeException {

    private final List<FieldError> errors;

    public ValidationException(List<FieldError> errors) {
        super("Request validation failed");
        this.errors = errors;
    }

    public List<FieldError> errors() {
        return errors;
    }
}

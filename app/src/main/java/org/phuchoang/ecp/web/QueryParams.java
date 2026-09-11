package org.phuchoang.ecp.web;

import jakarta.servlet.http.HttpServletRequest;
import org.phuchoang.ecp.sharedkernel.api.FieldErrorCodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * An unknown query parameter is a validation failure, never silently ignored (Integration
 * Contract.md §3.3 — "this is CQRS working as intended: a read model is purpose-built"). Each
 * endpoint calls {@link #rejectUnknown} with the exact set of parameters it documents.
 */
public final class QueryParams {

    private QueryParams() {
    }

    public static void rejectUnknown(HttpServletRequest request, Set<String> allowed) {
        List<FieldError> errors = new ArrayList<>();
        request.getParameterMap().keySet().forEach(param -> {
            if (!allowed.contains(param)) {
                errors.add(new FieldError(param, FieldErrorCodes.CONSTRAINT_VIOLATED, "Unknown parameter."));
            }
        });
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}

package org.phuchoang.ecp.web.common.request;

import org.phuchoang.ecp.sharedkernel.api.error.FieldErrorCodes;
import org.phuchoang.ecp.web.common.error.FieldError;
import org.phuchoang.ecp.web.common.error.ValidationException;

import java.util.ArrayList;
import java.util.List;

/** Presence checks for request bodies that are not bean-validated; failures become one problem+json body. */
public final class RequestValidation {

    private RequestValidation() {
    }

    /** Rejects a single missing field immediately. */
    public static void requireNonBlank(String value, String field) {
        List<FieldError> errors = new ArrayList<>();
        requireNonBlank(value, field, errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    /** Collects a missing field so several can be reported together. */
    public static void requireNonBlank(String value, String field, List<FieldError> errors) {
        if (value == null || value.isBlank()) {
            errors.add(new FieldError(field, FieldErrorCodes.REQUIRED, field + " is required."));
        }
    }

    public static void throwIfAny(List<FieldError> errors) {
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}

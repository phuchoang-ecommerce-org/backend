package org.phuchoang.ecp.web;

import org.phuchoang.ecp.sharedkernel.api.FieldErrorCodes;

import java.util.List;
import java.util.Set;

/**
 * {@code ?sort=field:asc|desc} (Integration Contract.md §3.3). Only fields an endpoint documents as
 * sortable are accepted — each caller passes its own allow-list, since "sortable" is a per-endpoint
 * decision backed by a specific index (`P10`), not a platform-wide one.
 */
public record SortSpec(String field, boolean descending) {

    public static SortSpec parse(String raw, Set<String> allowedFields) {
        String[] parts = raw.split(":", 2);
        String field = parts[0];
        String direction = parts.length > 1 ? parts[1] : "asc";
        if (!allowedFields.contains(field)) {
            throw new ValidationException(List.of(
                new FieldError("sort", FieldErrorCodes.CONSTRAINT_VIOLATED, "Field is not sortable.")));
        }
        if (!direction.equals("asc") && !direction.equals("desc")) {
            throw new ValidationException(List.of(
                new FieldError("sort", FieldErrorCodes.CONSTRAINT_VIOLATED, "Direction must be 'asc' or 'desc'.")));
        }
        return new SortSpec(field, direction.equals("desc"));
    }
}

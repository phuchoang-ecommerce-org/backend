package org.phuchoang.ecp.sharedkernel.api.cursor;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** A decoded keyset position: ordered sort values and the UUID tie-breaker. */
public record CursorPosition(List<CursorValue> sortValues, UUID tieBreaker) {

    public CursorPosition {
        sortValues = List.copyOf(sortValues);
        if (sortValues.isEmpty()) {
            throw new IllegalArgumentException("A cursor requires at least one sort value.");
        }
        Objects.requireNonNull(tieBreaker, "tieBreaker");
    }
}

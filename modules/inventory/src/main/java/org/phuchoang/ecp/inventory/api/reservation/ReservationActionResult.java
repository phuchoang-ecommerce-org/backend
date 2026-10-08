package org.phuchoang.ecp.inventory.api.reservation;

import java.util.Objects;

/** Idempotent release/commit outcome; callers can distinguish a no-op from a completed transition. */
public record ReservationActionResult(ReservationView reservation, Outcome outcome) {
    public ReservationActionResult {
        Objects.requireNonNull(reservation, "reservation");
        Objects.requireNonNull(outcome, "outcome");
    }

    public enum Outcome {
        RELEASED,
        ALREADY_RELEASED,
        COMMITTED,
        ALREADY_COMMITTED,
        DECLINED_ALREADY_COMMITTED,
        DECLINED_ALREADY_RELEASED
    }
}

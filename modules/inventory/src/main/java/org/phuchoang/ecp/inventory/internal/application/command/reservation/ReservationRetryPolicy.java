package org.phuchoang.ecp.inventory.internal.application.command.reservation;

/**
 * Application-level limit for retrying a full reservation state transition after an optimistic
 * locking conflict. The bound applies equally to reserve, release, and commit.
 */
public record ReservationRetryPolicy(int maxAttempts) {

    public ReservationRetryPolicy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("Reservation retry attempts must be at least 1.");
        }
    }
}

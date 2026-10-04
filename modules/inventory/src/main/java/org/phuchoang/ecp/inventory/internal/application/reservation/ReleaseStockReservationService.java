package org.phuchoang.ecp.inventory.internal.application.reservation;

import org.phuchoang.ecp.inventory.api.reservation.ReservationActionResult;
import org.phuchoang.ecp.inventory.api.reservation.ReservationReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

/** Idempotent release with a bounded retry if a concurrent resolution wins the first attempt. */
@Service
public class ReleaseStockReservationService {

    private final ReleaseStockReservationAttempt attempt;
    private final int maxAttempts;

    public ReleaseStockReservationService(ReleaseStockReservationAttempt attempt,
            @Value("${ecp.inventory.reservation.max-attempts:8}") int maxAttempts) {
        this.attempt = attempt;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    public ReservationActionResult release(ReservationReference reservation, boolean orderExists) {
        OptimisticLockingFailureException last = null;
        for (int number = 1; number <= maxAttempts; number++) {
            try {
                return attempt.release(reservation, orderExists);
            } catch (OptimisticLockingFailureException race) {
                last = race;
            }
        }
        throw last;
    }
}

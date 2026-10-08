package org.phuchoang.ecp.inventory.internal.application.command.reservation.release;

import org.phuchoang.ecp.inventory.internal.domain.service.ReservationRetryPolicy;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

/** Idempotent release with bounded retries if a concurrent resolution wins the first attempt. */
@Service
public class ReleaseStockReservationService {

    private final ReleaseStockReservationAttempt attempt;
    private final int maxAttempts;

    public ReleaseStockReservationService(ReleaseStockReservationAttempt attempt,
            ReservationRetryPolicy retryPolicy) {
        this.attempt = attempt;
        this.maxAttempts = retryPolicy.maxAttempts();
    }

    public ReleaseStockReservationResult release(ReleaseStockReservationRequest request) {
        OptimisticLockingFailureException last = null;
        for (int number = 1; number <= maxAttempts; number++) {
            try {
                return attempt.release(request);
            } catch (OptimisticLockingFailureException race) {
                last = race;
            }
        }
        throw last;
    }
}

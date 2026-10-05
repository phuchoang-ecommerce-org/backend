package org.phuchoang.ecp.inventory.internal.application.command.reservation.commit;

import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

/** Authorised, idempotent fulfilment commitment. */
@Service
public class CommitStockReservationService {

    private static final String COMMIT_STOCK_RESERVATION = "commitStockReservation";

    private final IdentityAuthorization authorization;
    private final CommitStockReservationAttempt attempt;
    private final int maxAttempts;

    public CommitStockReservationService(IdentityAuthorization authorization, CommitStockReservationAttempt attempt,
            @Value("${ecp.inventory.reservation.max-attempts:8}") int maxAttempts) {
        this.authorization = authorization;
        this.attempt = attempt;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    public CommitStockReservationResult commit(CommitStockReservationRequest request) {
        authorization.assertAuthorized(request.caller(), COMMIT_STOCK_RESERVATION);
        OptimisticLockingFailureException last = null;
        for (int number = 1; number <= maxAttempts; number++) {
            try {
                return attempt.commit(request);
            } catch (OptimisticLockingFailureException race) {
                last = race;
            }
        }
        throw last;
    }
}

package org.phuchoang.ecp.inventory.internal.application.reservation;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.inventory.api.reservation.ReservationReference;

import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CommitStockReservationServiceTest {

    @Test
    void assertsThePermissionMatrixOperationBeforeCommitting() {
        IdentityAuthorization authorization = mock(IdentityAuthorization.class);
        CommitStockReservationAttempt attempt = mock(CommitStockReservationAttempt.class);
        CommitStockReservationService service = new CommitStockReservationService(authorization, attempt, 1);
        IdentityActor caller = new IdentityActor(UUID.randomUUID(), Set.of("WAREHOUSE_OPERATOR"));

        ReservationReference reference = new ReservationReference(UUID.randomUUID(), UUID.randomUUID());
        service.commit(caller, reference);

        verify(authorization).assertAuthorized(caller, "commitStockReservation");
        verify(attempt).commit(reference);
    }
}

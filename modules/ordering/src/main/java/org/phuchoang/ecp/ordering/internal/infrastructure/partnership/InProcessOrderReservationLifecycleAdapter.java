package org.phuchoang.ecp.ordering.internal.infrastructure.partnership;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.inventory.api.reservation.InventoryReservationFacade;
import org.phuchoang.ecp.inventory.api.reservation.ReservationReference;
import org.phuchoang.ecp.ordering.internal.application.lifecycle.command.OrderReservationLifecyclePort;
import org.phuchoang.ecp.ordering.internal.domain.model.OrderReservation;
import org.springframework.stereotype.Component;

/** In-process implementation of Ordering's lifecycle reservation port. */
@Component
@RequiredArgsConstructor
class InProcessOrderReservationLifecycleAdapter implements OrderReservationLifecyclePort {
    private final InventoryReservationFacade inventory;

    @Override
    public void commit(IdentityActor actor, OrderReservation reservation) {
        inventory.commitStockReservation(actor, new ReservationReference(reservation.stockItemId(), reservation.reservationId()));
    }

    @Override
    public void release(OrderReservation reservation) {
        inventory.release(new ReservationReference(reservation.stockItemId(), reservation.reservationId()), true);
    }
}

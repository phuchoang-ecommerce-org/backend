package org.phuchoang.ecp.ordering.internal.application.lifecycle.command;

import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.ordering.internal.domain.model.OrderReservation;

/** Ordering-owned port for lifecycle effects on Inventory-held reservations. */
public interface OrderReservationLifecyclePort {
    void commit(IdentityActor actor, OrderReservation reservation);
    void release(OrderReservation reservation);
}

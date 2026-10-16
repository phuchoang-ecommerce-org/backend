package org.phuchoang.ecp.ordering.internal.application.lifecycle.command;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.audit.api.AuditTrail;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.ordering.internal.domain.model.Order;
import org.phuchoang.ecp.ordering.internal.domain.model.OrderStatus;
import org.phuchoang.ecp.ordering.internal.domain.service.OrderLifecycleDomainService;

import java.time.Clock;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdvanceOrderStatusServiceTest {
    @Test
    void commitsReservationsBeforePersistingThePackedTransitionAndAuditsIt() {
        IdentityAuthorization authorization = mock(IdentityAuthorization.class);
        OrderLifecycleDomainService lifecycle = mock(OrderLifecycleDomainService.class);
        OrderReservationLifecyclePort reservations = mock(OrderReservationLifecyclePort.class);
        AuditTrail audit = mock(AuditTrail.class);
        Order order = mock(Order.class);
        UUID orderId = UUID.randomUUID();
        when(order.id()).thenReturn(orderId);
        when(order.status()).thenReturn(OrderStatus.PROCESSING);
        when(order.reservations()).thenReturn(java.util.List.of());
        var transition = new OrderLifecycleDomainService.Transition(order, order);
        when(lifecycle.prepareTransition(orderId, OrderStatus.PACKED)).thenReturn(transition);
        when(lifecycle.persist(transition)).thenReturn(order);
        IdentityActor actor = new IdentityActor(UUID.randomUUID(), java.util.Set.of("WAREHOUSE_OPERATOR"));

        new AdvanceOrderStatusService(authorization, lifecycle, reservations, audit, Clock.systemUTC())
            .advance(new AdvanceOrderStatusCommand(actor, orderId, OrderStatus.PACKED, "packed", UUID.randomUUID()));

        verify(authorization).assertAuthorized(actor, "advanceOrderStatus");
        verify(lifecycle).persist(transition);
        verify(audit).record(org.mockito.ArgumentMatchers.any());
    }
}

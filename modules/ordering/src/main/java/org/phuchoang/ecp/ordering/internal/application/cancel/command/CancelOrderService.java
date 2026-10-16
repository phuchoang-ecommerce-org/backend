package org.phuchoang.ecp.ordering.internal.application.cancel.command;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.audit.api.AuditRecord;
import org.phuchoang.ecp.audit.api.AuditTrail;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.ordering.internal.application.lifecycle.command.OrderReservationLifecyclePort;
import org.phuchoang.ecp.ordering.internal.domain.model.Order;
import org.phuchoang.ecp.ordering.internal.domain.model.OrderStatus;
import org.phuchoang.ecp.ordering.internal.domain.service.OrderLifecycleDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Map;

/** UC-ORD-08. Cancellation is durable even if releasing its hold must be retried. */
@Service
@RequiredArgsConstructor
public class CancelOrderService {
    private static final Logger log = LoggerFactory.getLogger(CancelOrderService.class);
    private final IdentityAuthorization authorization;
    private final OrderLifecycleDomainService lifecycle;
    private final OrderReservationLifecyclePort reservations;
    private final AuditTrail audit;
    private final Clock clock;

    @Transactional
    public Order cancel(CancelOrderCommand command) {
        authorization.assertAuthorized(command.caller(), "cancelOrder");
        Order current = lifecycle.require(command.orderId());
        assertOwnerOrPrivileged(command.caller(), current);
        if (current.status() == OrderStatus.CANCELLED) return current;
        var transition = lifecycle.prepareTransition(command.orderId(), OrderStatus.CANCELLED);
        Order cancelled = lifecycle.persist(transition);
        audit.record(new AuditRecord(java.util.UUID.randomUUID(), command.caller().accountId(), role(command.caller()), "ORDER_CANCELLED",
            "Order", cancelled.id(), "ordering", Map.of("status", current.status().name()), Map.of("status", "CANCELLED"),
            command.reason(), command.correlationId(), clock.instant(), null, false));
        current.reservations().forEach(reservation -> releaseAfterCancellation(reservation, cancelled.id()));
        return cancelled;
    }

    private void releaseAfterCancellation(org.phuchoang.ecp.ordering.internal.domain.model.OrderReservation reservation,
            java.util.UUID orderId) {
        try {
            reservations.release(reservation);
        } catch (RuntimeException exception) {
            // UC-ORD-08 E3: the customer-facing cancellation stands; operations can retry the held stock safely.
            log.error("Reservation release requires retry after cancelling order {}", orderId, exception);
        }
    }

    private static void assertOwnerOrPrivileged(IdentityActor caller, Order order) {
        if (caller.accountId().equals(order.customerId()) || caller.roles().contains("CUSTOMER_SUPPORT")
                || caller.roles().contains("ADMINISTRATOR")) return;
        throw new OrderLifecycleDomainService.OrderNotFound(order.id());
    }

    private static String role(IdentityActor caller) { return caller.roles().stream().sorted().findFirst().orElse("UNKNOWN"); }
}

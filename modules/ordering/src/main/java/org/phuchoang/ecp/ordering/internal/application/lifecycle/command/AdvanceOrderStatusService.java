package org.phuchoang.ecp.ordering.internal.application.lifecycle.command;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.audit.api.AuditRecord;
import org.phuchoang.ecp.audit.api.AuditTrail;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.ordering.internal.domain.model.Order;
import org.phuchoang.ecp.ordering.internal.domain.model.OrderStatus;
import org.phuchoang.ecp.ordering.internal.domain.service.OrderLifecycleDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Map;

/** UC-ORD-10: authorization is per operation; the aggregate decides whether the requested edge exists. */
@Service
@RequiredArgsConstructor
public class AdvanceOrderStatusService {
    private final IdentityAuthorization authorization;
    private final OrderLifecycleDomainService lifecycle;
    private final OrderReservationLifecyclePort reservations;
    private final AuditTrail audit;
    private final Clock clock;

    @Transactional
    public Order advance(AdvanceOrderStatusCommand command) {
        authorization.assertAuthorized(command.caller(), "advanceOrderStatus");
        var transition = lifecycle.prepareTransition(command.orderId(), command.target());
        Order before = transition.before();
        // Side effects that must be atomic with the edge happen before the persisted transition. Any failure rolls back.
        if (command.target() == OrderStatus.PACKED) {
            before.reservations().forEach(reservation -> reservations.commit(command.caller(), reservation));
        }
        Order after = lifecycle.persist(transition);
        audit.record(new AuditRecord(java.util.UUID.randomUUID(), command.caller().accountId(), role(command), "ORDER_STATUS_ADVANCED",
            "Order", after.id(), "ordering", Map.of("status", before.status().name()), Map.of("status", after.status().name()),
            command.reason(), command.correlationId(), clock.instant(), null, false));
        return after;
    }

    private static String role(AdvanceOrderStatusCommand command) {
        return command.caller().roles().stream().sorted().findFirst().orElse("UNKNOWN");
    }
}

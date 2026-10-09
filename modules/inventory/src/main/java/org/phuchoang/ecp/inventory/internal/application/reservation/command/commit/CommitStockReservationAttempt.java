package org.phuchoang.ecp.inventory.internal.application.reservation.command.commit;

import org.phuchoang.ecp.inventory.internal.domain.service.CommitStockReservationDomainService;
import org.phuchoang.ecp.inventory.internal.domain.model.StockReservation;
import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** One commit transition; failure leaves the aggregate's persisted reservation held. */
@Service
public class CommitStockReservationAttempt {

    private static final Logger log = LoggerFactory.getLogger(CommitStockReservationAttempt.class);

    private final CommitStockReservationDomainService reservations;

    public CommitStockReservationAttempt(CommitStockReservationDomainService reservations) {
        this.reservations = reservations;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CommitStockReservationResult commit(CommitStockReservationRequest request) {
        var transition = reservations.commit(request.stockItemId(), request.reservationId());
        if (transition.outcome() == StockItem.TransitionOutcome.DECLINED_ALREADY_RELEASED) {
            log.warn("Declined commitment of released reservation {} on stock item {}; warehouse resolution required.",
                request.reservationId(), request.stockItemId());
        }
        return toResult(transition);
    }

    private static CommitStockReservationResult toResult(StockItem.Transition transition) {
        StockReservation reservation = transition.reservation();
        return new CommitStockReservationResult(reservation.id(), transition.stockItem().id(), reservation.orderId(),
            reservation.orderLineId(), reservation.quantity(),
            CommitStockReservationResult.Status.valueOf(reservation.status().name()), reservation.expiresAt(),
            reservation.resolvedAt(), reservation.orphanedAt(),
            CommitStockReservationResult.Outcome.valueOf(transition.outcome().name()));
    }
}

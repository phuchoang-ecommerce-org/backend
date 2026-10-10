package org.phuchoang.ecp.inventory.internal.application.reservation.command.commit;

import org.phuchoang.ecp.inventory.internal.domain.service.CommitStockReservationDomainService;
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
    private final CommitStockReservationMapper mapper;

    public CommitStockReservationAttempt(CommitStockReservationDomainService reservations, CommitStockReservationMapper mapper) {
        this.reservations = reservations;
        this.mapper = mapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CommitStockReservationResult commit(CommitStockReservationRequest request) {
        var transition = reservations.commit(request.stockItemId(), request.reservationId());
        if (transition.outcome() == StockItem.TransitionOutcome.DECLINED_ALREADY_RELEASED) {
            log.warn("Declined commitment of released reservation {} on stock item {}; warehouse resolution required.",
                request.reservationId(), request.stockItemId());
        }
        return mapper.result(transition.stockItem(), transition.reservation(), transition.outcome());
    }
}

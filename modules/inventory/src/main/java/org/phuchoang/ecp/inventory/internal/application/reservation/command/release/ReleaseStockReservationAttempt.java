package org.phuchoang.ecp.inventory.internal.application.reservation.command.release;

import org.phuchoang.ecp.inventory.internal.domain.service.ReleaseStockReservationDomainService;
import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** One release transition; a persistence failure leaves the hold intact for retry. */
@Service
public class ReleaseStockReservationAttempt {

    private static final Logger log = LoggerFactory.getLogger(ReleaseStockReservationAttempt.class);

    private final ReleaseStockReservationDomainService reservations;
    private final ReleaseStockReservationMapper mapper;

    public ReleaseStockReservationAttempt(ReleaseStockReservationDomainService reservations, ReleaseStockReservationMapper mapper) {
        this.reservations = reservations;
        this.mapper = mapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReleaseStockReservationResult release(ReleaseStockReservationRequest request) {
        var transition = reservations.release(request.stockItemId(), request.reservationId(), !request.orderExists());
        if (transition.outcome() == StockItem.TransitionOutcome.DECLINED_ALREADY_COMMITTED) {
            log.warn("Declined release of committed reservation {} on stock item {}; investigation required.",
                request.reservationId(), request.stockItemId());
        }
        return mapper.result(transition.stockItem(), transition.reservation(), transition.outcome());
    }
}

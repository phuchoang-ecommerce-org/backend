package org.phuchoang.ecp.inventory.internal.application.command.reservation.release;

import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;
import org.phuchoang.ecp.inventory.internal.domain.model.StockReservation;
import org.phuchoang.ecp.inventory.internal.domain.repository.StockItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** One release transition; a persistence failure leaves the hold intact for retry. */
@Service
public class ReleaseStockReservationAttempt {

    private static final Logger log = LoggerFactory.getLogger(ReleaseStockReservationAttempt.class);

    private final StockItemRepository stockItems;
    private final Clock clock;

    public ReleaseStockReservationAttempt(StockItemRepository stockItems, Clock clock) {
        this.stockItems = stockItems;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReleaseStockReservationResult release(ReleaseStockReservationRequest request) {
        StockItem item = stockItems.findById(request.stockItemId())
            .orElseThrow(() -> new IllegalArgumentException("Unknown stock item " + request.stockItemId() + "."));
        StockItem.Transition transition = item.release(request.reservationId(), clock.instant(), !request.orderExists());
        if (transition.outcome() == StockItem.TransitionOutcome.DECLINED_ALREADY_COMMITTED) {
            log.warn("Declined release of committed reservation {} on stock item {}; investigation required.",
                request.reservationId(), request.stockItemId());
        }
        if (transition.stockItem() != item) {
            stockItems.saveAll(java.util.List.of(transition.stockItem()));
        }
        return toResult(transition);
    }

    private static ReleaseStockReservationResult toResult(StockItem.Transition transition) {
        StockReservation reservation = transition.reservation();
        return new ReleaseStockReservationResult(reservation.id(), transition.stockItem().id(), reservation.orderId(),
            reservation.orderLineId(), reservation.quantity(),
            ReleaseStockReservationResult.Status.valueOf(reservation.status().name()), reservation.expiresAt(),
            reservation.resolvedAt(), reservation.orphanedAt(),
            ReleaseStockReservationResult.Outcome.valueOf(transition.outcome().name()));
    }
}

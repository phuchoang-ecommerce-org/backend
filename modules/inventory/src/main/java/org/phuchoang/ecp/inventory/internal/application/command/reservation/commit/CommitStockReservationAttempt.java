package org.phuchoang.ecp.inventory.internal.application.command.reservation.commit;

import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;
import org.phuchoang.ecp.inventory.internal.domain.model.StockReservation;
import org.phuchoang.ecp.inventory.internal.domain.repository.StockItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** One commit transition; failure leaves the aggregate's persisted reservation held. */
@Service
public class CommitStockReservationAttempt {

    private static final Logger log = LoggerFactory.getLogger(CommitStockReservationAttempt.class);

    private final StockItemRepository stockItems;
    private final Clock clock;

    public CommitStockReservationAttempt(StockItemRepository stockItems, Clock clock) {
        this.stockItems = stockItems;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CommitStockReservationResult commit(CommitStockReservationRequest request) {
        StockItem item = stockItems.findById(request.stockItemId())
            .orElseThrow(() -> new IllegalArgumentException("Unknown stock item " + request.stockItemId() + "."));
        StockItem.Transition transition = item.commit(request.reservationId(), clock.instant());
        if (transition.outcome() == StockItem.TransitionOutcome.DECLINED_ALREADY_RELEASED) {
            log.warn("Declined commitment of released reservation {} on stock item {}; warehouse resolution required.",
                request.reservationId(), request.stockItemId());
        }
        if (transition.stockItem() != item) {
            stockItems.saveAll(java.util.List.of(transition.stockItem()));
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

package org.phuchoang.ecp.inventory.internal.application.reservation;

import org.phuchoang.ecp.inventory.api.reservation.ReservationActionResult;
import org.phuchoang.ecp.inventory.api.reservation.ReservationReference;
import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;
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
    public ReservationActionResult commit(ReservationReference reference) {
        StockItem item = stockItems.findById(reference.stockItemId())
            .orElseThrow(() -> new IllegalArgumentException("Unknown stock item " + reference.stockItemId() + "."));
        StockItem.Transition transition = item.commit(reference.reservationId(), clock.instant());
        if (transition.outcome() == StockItem.TransitionOutcome.DECLINED_ALREADY_RELEASED) {
            log.warn("Declined commitment of released reservation {} on stock item {}; warehouse resolution required.",
                reference.reservationId(), reference.stockItemId());
        }
        if (transition.stockItem() != item) {
            stockItems.saveAll(java.util.List.of(transition.stockItem()));
        }
        return new ReservationActionResult(ReservationViews.of(transition.stockItem(), transition.reservation()),
            ReservationViews.outcome(transition.outcome()));
    }
}

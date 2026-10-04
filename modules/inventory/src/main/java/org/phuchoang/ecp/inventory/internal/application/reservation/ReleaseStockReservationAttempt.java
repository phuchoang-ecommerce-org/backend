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

/** One release transition. A database failure rolls this transaction back, leaving the hold intact for retry. */
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
    public ReservationActionResult release(ReservationReference reference, boolean orderExists) {
        StockItem item = stockItems.findById(reference.stockItemId())
            .orElseThrow(() -> new IllegalArgumentException("Unknown stock item " + reference.stockItemId() + "."));
        StockItem.Transition transition = item.release(reference.reservationId(), clock.instant(), !orderExists);
        if (transition.outcome() == StockItem.TransitionOutcome.DECLINED_ALREADY_COMMITTED) {
            log.warn("Declined release of committed reservation {} on stock item {}; investigation required.",
                reference.reservationId(), reference.stockItemId());
        }
        if (transition.stockItem() != item) {
            stockItems.saveAll(java.util.List.of(transition.stockItem()));
        }
        return new ReservationActionResult(ReservationViews.of(transition.stockItem(), transition.reservation()),
            ReservationViews.outcome(transition.outcome()));
    }
}

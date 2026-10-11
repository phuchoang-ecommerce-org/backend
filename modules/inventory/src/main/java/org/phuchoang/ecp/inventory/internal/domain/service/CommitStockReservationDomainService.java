package org.phuchoang.ecp.inventory.internal.domain.service;

import org.jmolecules.ddd.annotation.Service;
import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;
import org.phuchoang.ecp.inventory.internal.domain.repository.StockItemRepository;

import java.time.Clock;
import java.util.UUID;

/**
 * Executes the authoritative commitment transition for one held stock
 * reservation.
 */
@Service
public final class CommitStockReservationDomainService {

  private final StockItemRepository stockItems;
  private final Clock clock;

  public CommitStockReservationDomainService(StockItemRepository stockItems, Clock clock) {
    this.stockItems = stockItems;
    this.clock = clock;
  }

  public StockItem.Transition commit(UUID stockItemId, UUID reservationId) {
    StockItem item = requireStockItem(stockItemId);
    return item.commit(reservationId, clock.instant(), stockItems);
  }

  private StockItem requireStockItem(UUID stockItemId) {
    return stockItems.findById(stockItemId)
        .orElseThrow(() -> new IllegalArgumentException("Unknown stock item " + stockItemId + "."));
  }
}

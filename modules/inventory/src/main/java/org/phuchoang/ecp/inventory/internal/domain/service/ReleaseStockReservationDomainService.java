package org.phuchoang.ecp.inventory.internal.domain.service;

import org.jmolecules.ddd.annotation.Service;
import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;
import org.phuchoang.ecp.inventory.internal.domain.repository.StockItemRepository;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Executes the authoritative release transition for one held stock reservation.
 */
@Service
public final class ReleaseStockReservationDomainService {

  private final StockItemRepository stockItems;
  private final Clock clock;

  public ReleaseStockReservationDomainService(StockItemRepository stockItems, Clock clock) {
    this.stockItems = stockItems;
    this.clock = clock;
  }

  public StockItem.Transition release(UUID stockItemId, UUID reservationId, boolean orphaned) {
    StockItem item = requireStockItem(stockItemId);
    StockItem.Transition transition = item.release(reservationId, clock.instant(), orphaned);
    if (transition.stockItem() != item) {
      stockItems.saveAll(List.of(transition.stockItem()));
    }
    return transition;
  }

  private StockItem requireStockItem(UUID stockItemId) {
    return stockItems.findById(stockItemId)
        .orElseThrow(() -> new IllegalArgumentException("Unknown stock item " + stockItemId + "."));
  }
}

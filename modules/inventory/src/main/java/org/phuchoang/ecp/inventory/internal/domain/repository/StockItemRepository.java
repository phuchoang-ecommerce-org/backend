package org.phuchoang.ecp.inventory.internal.domain.repository;

import org.jmolecules.ddd.annotation.Repository;
import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Aggregate-oriented persistence boundary for StockItem and its child reservations. */
@Repository
public interface StockItemRepository {

    Optional<StockItem> findById(UUID stockItemId);

    List<StockItem> findByIds(Collection<UUID> stockItemIds);

    List<StockItem> findByOrderId(UUID orderId);

    void saveAll(Collection<StockItem> stockItems);
}

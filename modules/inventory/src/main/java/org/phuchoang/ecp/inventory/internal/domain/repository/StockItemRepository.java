package org.phuchoang.ecp.inventory.internal.domain.repository;

import org.jmolecules.ddd.annotation.Repository;
import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Aggregate-oriented write boundary for StockItem and all of its child entities. */
@Repository
public interface StockItemRepository {

    Optional<StockItem> findById(UUID stockItemId);

    List<StockItem> findByIds(Collection<UUID> stockItemIds);

    List<StockItem> findByOrderId(UUID orderId);

    void saveAll(Collection<StockItem> stockItems);

    /** Persists a root-owned adjustment together with its changed StockItem. */
    void save(StockItem.Adjustment adjustment);
}

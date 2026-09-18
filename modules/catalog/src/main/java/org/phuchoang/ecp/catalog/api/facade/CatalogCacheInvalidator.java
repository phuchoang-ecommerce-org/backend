package org.phuchoang.ecp.catalog.api.facade;

import java.util.List;
import java.util.UUID;

/**
 * Semantic invalidation of Catalog's read caches, for the composition root's event consumer
 * (Backend Architecture.md §5.7: invalidation from a Kafka event is a consumer, and it
 * {@code DEL}s, never writes). Callers name what changed; only Catalog knows the physical keys.
 */
public interface CatalogCacheInvalidator {

    void productChanged(UUID productId);

    void variantsChanged(List<UUID> variantIds);

    void categoryTreeChanged();

    void categoryListingsChanged(List<UUID> categoryIds);
}

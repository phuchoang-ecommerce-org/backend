package org.phuchoang.ecp.catalog.internal.infrastructure.search;

import java.util.Map;

/** A Catalog event reduced to the smallest Elasticsearch mutation it requires. */
sealed interface SearchProjectionOperation permits SearchProjectionOperation.ProductSnapshot,
        SearchProjectionOperation.VariantPriceChange, SearchProjectionOperation.VariantAddition,
        SearchProjectionOperation.ProductTombstone, SearchProjectionOperation.Ignore {

    record ProductSnapshot(Map<String, Object> document) implements SearchProjectionOperation {
    }

    record VariantPriceChange(Map<String, Object> variant) implements SearchProjectionOperation {
    }

    record VariantAddition(Map<String, Object> variant) implements SearchProjectionOperation {
    }

    record ProductTombstone() implements SearchProjectionOperation {
    }

    record Ignore(String eventType) implements SearchProjectionOperation {
    }
}

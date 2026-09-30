package org.phuchoang.ecp.catalog.api.search.view;

import java.util.List;

/** One searchable field's available values and their matching-product counts. */
public record SearchFacetView(String field, List<Value> values) {

    public record Value(String value, long count) {
    }
}

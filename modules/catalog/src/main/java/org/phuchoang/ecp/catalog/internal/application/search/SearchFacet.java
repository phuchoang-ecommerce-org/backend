package org.phuchoang.ecp.catalog.internal.application.search;

import java.util.List;

/** A facet aggregation calculated by the search store. */
public record SearchFacet(String field, List<Value> values) {

    public record Value(String value, long count) {
    }
}

package org.phuchoang.ecp.catalog.internal.application.search.query;

/** A filter that the query actually applied. */
public record ActiveSearchFilter(String field, String value) {
}

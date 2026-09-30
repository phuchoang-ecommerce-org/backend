package org.phuchoang.ecp.catalog.api.search.view;

/** An exact filter the read model applied; no active filter is silently dropped. */
public record ActiveSearchFilterView(String field, String value) {
}

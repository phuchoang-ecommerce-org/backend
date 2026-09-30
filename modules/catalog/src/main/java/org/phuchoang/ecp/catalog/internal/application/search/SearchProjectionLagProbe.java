package org.phuchoang.ecp.catalog.internal.application.search;

/** Read-only operational view of the product-search projection's newest applied Catalog event. */
public interface SearchProjectionLagProbe {

    /** Seconds behind the newest Catalog event applied to the alias; {@code NaN} means no mark yet. */
    double lagSeconds();
}

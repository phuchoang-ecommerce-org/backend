package org.phuchoang.ecp.cart.internal.domain.model;

/** Lifecycle owned by the Cart aggregate. */
public enum CartStatus {
    ACTIVE, CHECKED_OUT, EXPIRED, MERGED
}

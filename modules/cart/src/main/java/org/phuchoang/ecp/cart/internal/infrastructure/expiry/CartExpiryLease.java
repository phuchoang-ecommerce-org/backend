package org.phuchoang.ecp.cart.internal.infrastructure.expiry;

/** Cross-replica lease for the expiry sweep. */
public interface CartExpiryLease {
    boolean executeIfAcquired(Runnable operation);
}

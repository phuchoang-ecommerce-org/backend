package org.phuchoang.ecp.promotion.internal.infrastructure.lifecycle;

/** Cross-replica lease for promotion lifecycle work. */
public interface PromotionLifecycleLease {
    boolean executeIfAcquired(Runnable operation);
}

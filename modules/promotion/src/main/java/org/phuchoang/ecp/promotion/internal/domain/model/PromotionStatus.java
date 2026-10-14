package org.phuchoang.ecp.promotion.internal.domain.model;

/** Lifecycle state used when deciding whether a promotion may be evaluated. */
public enum PromotionStatus {
    DRAFT,
    ACTIVE,
    PAUSED,
    EXPIRED
}

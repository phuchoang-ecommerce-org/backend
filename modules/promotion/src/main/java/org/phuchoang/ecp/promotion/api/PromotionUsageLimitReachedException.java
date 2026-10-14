package org.phuchoang.ecp.promotion.api;

import org.phuchoang.ecp.promotion.api.error.DomainException;

/** Expected conflict when another transaction consumes the last promotion slot. */
public final class PromotionUsageLimitReachedException extends DomainException {
    public PromotionUsageLimitReachedException() { super(PromotionErrorCode.USAGE_LIMIT_REACHED, "Promotion is no longer available."); }
}

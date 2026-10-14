package org.phuchoang.ecp.promotion.internal.domain.model;

/** Expected business outcome when the aggregate has no remaining redemption slots. */
public final class PromotionUsageExhaustedException extends RuntimeException {
    public PromotionUsageExhaustedException() {
        super("Promotion usage limit has been reached.");
    }
}

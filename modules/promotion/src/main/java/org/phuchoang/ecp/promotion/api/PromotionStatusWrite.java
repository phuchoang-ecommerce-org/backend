package org.phuchoang.ecp.promotion.api;

import java.time.Instant;

/** HTTP-neutral lifecycle request; an effective time schedules rather than immediately applies the transition. */
public record PromotionStatusWrite(String status, String reason, Instant effectiveAt) { }

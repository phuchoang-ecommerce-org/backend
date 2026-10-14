package org.phuchoang.ecp.promotion.internal.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromotionTest {

    private static final Instant NOW = Instant.parse("2026-10-14T08:00:00Z");

    @Test
    void rejectsAnUnboundedPromotionBeforeItCanBeCreated() {
        assertThrows(IllegalArgumentException.class, () -> Promotion.draft(
            UUID.randomUUID(), "Fall voucher", DiscountType.FIXED_AMOUNT, new BigDecimal("10.00"), "USD",
            null, null, NOW.plus(1, ChronoUnit.HOURS), NOW.plus(1, ChronoUnit.DAYS), null, 1, 0, false));
    }

    @Test
    void capsFixedDiscountAtTheDiscountableValue() {
        Promotion promotion = activeFixedPromotion(new BigDecimal("100.00"), 2);

        Promotion.Evaluation evaluation = promotion.evaluate(UUID.randomUUID(), new BigDecimal("25.00"), NOW);

        assertTrue(evaluation.eligible());
        assertEquals(new BigDecimal("25.0000"), evaluation.discountAmount());
    }

    @Test
    void rejectsFurtherRedemptionAfterUsageLimitIsReached() {
        Promotion promotion = activeFixedPromotion(new BigDecimal("10.00"), 1);
        UUID customer = UUID.randomUUID();

        Promotion claimed = promotion.claim(UUID.randomUUID(), customer, new BigDecimal("20.00"), NOW);

        assertEquals(1, claimed.usageCount());
        assertFalse(claimed.evaluate(UUID.randomUUID(), new BigDecimal("20.00"), NOW).eligible());
        assertThrows(PromotionUsageExhaustedException.class,
            () -> claimed.claim(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("20.00"), NOW));
    }

    private static Promotion activeFixedPromotion(BigDecimal amount, int usageLimit) {
        return Promotion.draft(UUID.randomUUID(), "Fall voucher", DiscountType.FIXED_AMOUNT, amount, "USD",
            null, null, NOW.minus(1, ChronoUnit.HOURS), NOW.plus(1, ChronoUnit.HOURS), usageLimit, 1, 0, false)
            .activate(NOW);
    }
}

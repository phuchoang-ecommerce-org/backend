package org.phuchoang.ecp.promotion.internal.domain.model;

import org.phuchoang.ecp.promotion.internal.domain.repository.PromotionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void reactivatesAnExpiredButStillValidPromotion() {
        Promotion promotion = activeFixedPromotion(new BigDecimal("10.00"), 2)
            .expire();

        Promotion reactivated = promotion.activate(NOW);

        assertEquals(PromotionStatus.ACTIVE, reactivated.status());
        assertTrue(reactivated.evaluate(UUID.randomUUID(), new BigDecimal("20.00"), NOW).eligible());
    }

    @Test
    void rejectsAnExpiredPromotionEvenWhenADeactivationSweepHasNotRun() {
        Promotion promotion = activeFixedPromotion(new BigDecimal("10.00"), 2);

        assertFalse(promotion.evaluate(UUID.randomUUID(), new BigDecimal("20.00"), NOW.plus(2, ChronoUnit.HOURS)).eligible());
    }

    @Test
    void expiresItselfWhenTheLastPermittedUseIsClaimed() {
        Promotion promotion = activeFixedPromotion(new BigDecimal("10.00"), 1);

        Promotion exhausted = promotion.claim(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("20.00"), NOW);

        assertEquals(PromotionStatus.EXPIRED, exhausted.status());
        assertFalse(exhausted.evaluate(UUID.randomUUID(), new BigDecimal("20.00"), NOW).eligible());
    }

    @Test
    void appliesAScheduledStatusOnlyWhenItBecomesDue() {
        Promotion promotion = activeFixedPromotion(new BigDecimal("10.00"), 2)
            .scheduleStatus(PromotionStatus.PAUSED, NOW.plus(1, ChronoUnit.HOURS), NOW);

        assertEquals(PromotionStatus.ACTIVE, promotion.applyScheduledStatusAt(NOW).status());
        assertEquals(PromotionStatus.PAUSED,
            promotion.applyScheduledStatusAt(NOW.plus(1, ChronoUnit.HOURS)).status());
    }

    @Test
    void lifecycleCommandLoadsTransitionsAndPersistsThroughTheAggregateRepository() {
        Promotion promotion = activeFixedPromotion(new BigDecimal("10.00"), 2);
        PromotionRepository promotions = mock(PromotionRepository.class);
        when(promotions.findById(promotion.id())).thenReturn(java.util.Optional.of(promotion));

        Promotion paused = Promotion.changeStatus(
            new Promotion.ChangeStatus(promotion.id(), PromotionStatus.PAUSED, NOW), promotions);

        assertEquals(PromotionStatus.PAUSED, paused.status());
        verify(promotions).save(paused);
    }

    private static Promotion activeFixedPromotion(BigDecimal amount, int usageLimit) {
        return Promotion.draft(UUID.randomUUID(), "Fall voucher", DiscountType.FIXED_AMOUNT, amount, "USD",
            null, null, NOW.minus(1, ChronoUnit.HOURS), NOW.plus(1, ChronoUnit.HOURS), usageLimit, 1, 0, false)
            .activate(NOW);
    }
}

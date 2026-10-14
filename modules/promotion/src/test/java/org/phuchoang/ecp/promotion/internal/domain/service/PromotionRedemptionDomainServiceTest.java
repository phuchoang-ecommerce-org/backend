package org.phuchoang.ecp.promotion.internal.domain.service;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.promotion.internal.domain.model.DiscountType;
import org.phuchoang.ecp.promotion.internal.domain.model.Promotion;
import org.phuchoang.ecp.promotion.internal.domain.model.PromotionRedemption;
import org.phuchoang.ecp.promotion.internal.domain.repository.PromotionRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PromotionRedemptionDomainServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-14T08:00:00Z");

    @Test
    void claimsTheAggregateThenRecordsTheRootAndRedemptionTogether() {
        PromotionRepository promotions = mock(PromotionRepository.class);
        Promotion promotion = activePromotion(2);
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(promotions.findForRedemption(promotion.id())).thenReturn(java.util.Optional.of(promotion));
        when(promotions.redemptionCountForCustomer(promotion.id(), customerId)).thenReturn(0L);

        Promotion.Redemption result = new PromotionRedemptionDomainService(promotions).redeem(
            new Promotion.RedeemPromotion(promotion.id(), orderId, customerId,
                new BigDecimal("25.00"), NOW));

        assertEquals(new BigDecimal("10.0000"), result.discountAmount());
        org.mockito.ArgumentCaptor<Promotion> claimed = org.mockito.ArgumentCaptor.forClass(Promotion.class);
        org.mockito.ArgumentCaptor<PromotionRedemption> redemption = org.mockito.ArgumentCaptor.forClass(PromotionRedemption.class);
        verify(promotions).recordRedemption(claimed.capture(), redemption.capture());
        assertEquals(1, claimed.getValue().usageCount());
        assertEquals(orderId, redemption.getValue().orderId());
    }

    @Test
    void rejectsWhenTheCustomerHasAlreadyReachedTheirLimit() {
        PromotionRepository promotions = mock(PromotionRepository.class);
        Promotion promotion = activePromotion(2);
        UUID customerId = UUID.randomUUID();
        when(promotions.findForRedemption(promotion.id())).thenReturn(java.util.Optional.of(promotion));
        when(promotions.redemptionCountForCustomer(promotion.id(), customerId)).thenReturn(1L);

        assertThrows(PromotionRedemptionDomainService.PromotionRedemptionUnavailableException.class,
            () -> new PromotionRedemptionDomainService(promotions).redeem(
                new Promotion.RedeemPromotion(promotion.id(), UUID.randomUUID(), customerId,
                    new BigDecimal("25.00"), NOW)));
    }

    private static Promotion activePromotion(int usageLimit) {
        return Promotion.draft(UUID.randomUUID(), "Fall voucher", DiscountType.FIXED_AMOUNT, new BigDecimal("10.00"),
            "USD", null, null, NOW.minus(1, ChronoUnit.HOURS), NOW.plus(1, ChronoUnit.HOURS), usageLimit, 1, 0, false)
            .activate(NOW);
    }
}

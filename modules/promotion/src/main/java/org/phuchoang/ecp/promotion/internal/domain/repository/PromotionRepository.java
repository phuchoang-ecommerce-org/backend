package org.phuchoang.ecp.promotion.internal.domain.repository;

import org.jmolecules.ddd.annotation.Repository;
import org.phuchoang.ecp.promotion.internal.domain.model.Promotion;
import org.phuchoang.ecp.promotion.internal.domain.model.PromotionRedemption;
import org.phuchoang.ecp.promotion.internal.domain.model.PromotionVoucher;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import java.util.UUID;

/** Aggregate persistence boundary; locked loading is the redemption concurrency strategy. */
@Repository
public interface PromotionRepository {
    Optional<Promotion> findById(UUID id);
    Optional<Promotion> findForRedemption(UUID id);
    List<Promotion> findDueScheduledStatusChanges(Instant now, int limit);
    List<Promotion> findDueActivations(Instant now, int limit);
    List<Promotion> findDueExpirations(Instant now, int limit);
    long redemptionCountForCustomer(UUID promotionId, UUID customerId);
    void save(Promotion promotion);
    void recordRedemption(Promotion promotion, PromotionRedemption redemption);
    void appendVouchers(Promotion promotion, List<PromotionVoucher> vouchers);
}

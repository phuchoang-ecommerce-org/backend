package org.phuchoang.ecp.promotion.internal.domain.service;

import org.jmolecules.ddd.annotation.Service;
import org.phuchoang.ecp.promotion.internal.domain.model.Promotion;
import org.phuchoang.ecp.promotion.internal.domain.repository.PromotionRepository;

/** Invokes the Promotion root's create-and-persist operation. */
@Service
public final class PromotionCreationDomainService {

    private final PromotionRepository promotions;

    public PromotionCreationDomainService(PromotionRepository promotions) {
        this.promotions = promotions;
    }

    public Promotion create(Promotion.CreatePromotion request) {
        return Promotion.create(request, promotions);
    }
}

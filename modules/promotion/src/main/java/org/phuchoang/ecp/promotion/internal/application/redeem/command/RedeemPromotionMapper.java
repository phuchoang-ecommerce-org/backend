package org.phuchoang.ecp.promotion.internal.application.redeem.command;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.promotion.internal.domain.model.Promotion;

import java.time.Instant;

/** Maps the redeem use case to the locked domain transition and its result. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface RedeemPromotionMapper {

    @Mapping(target = "now", source = "now")
    Promotion.RedeemPromotion domainRequest(RedeemPromotionCommand command, Instant now);

    RedeemedPromotion redeemedPromotion(Promotion.Redemption redemption);
}

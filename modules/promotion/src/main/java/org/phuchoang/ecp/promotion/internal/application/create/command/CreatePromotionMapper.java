package org.phuchoang.ecp.promotion.internal.application.create.command;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.promotion.internal.domain.model.Promotion;

import java.time.Instant;

/** Maps the create-promotion use case at its application-to-domain boundary. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface CreatePromotionMapper {

    @Mapping(target = "now", source = "now")
    Promotion.CreatePromotion domainRequest(CreatePromotionCommand command, Instant now);

    CreatedPromotion createdPromotion(Promotion promotion);
}

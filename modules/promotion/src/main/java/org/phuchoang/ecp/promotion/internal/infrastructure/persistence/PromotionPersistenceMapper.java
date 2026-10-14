package org.phuchoang.ecp.promotion.internal.infrastructure.persistence;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.promotion.internal.domain.model.Promotion;
import org.phuchoang.ecp.promotion.internal.domain.model.PromotionRedemption;
import org.phuchoang.ecp.promotion.internal.domain.model.PromotionVoucher;

/** MapStruct mapping between Promotion domain values and the local JPA representation. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface PromotionPersistenceMapper {

    @Mapping(target = "activate", ignore = true)
    @Mapping(target = "applyScheduledStatusAt", ignore = true)
    Promotion toDomain(PromotionEntity entity);

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(Promotion promotion, @MappingTarget PromotionEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    PromotionRedemptionEntity toEntity(PromotionRedemption redemption);

    @Mapping(target = "createdAt", ignore = true)
    PromotionVoucherEntity toEntity(PromotionVoucher voucher);
}

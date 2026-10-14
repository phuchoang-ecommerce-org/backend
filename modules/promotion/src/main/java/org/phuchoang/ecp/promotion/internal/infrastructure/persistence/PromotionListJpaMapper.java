package org.phuchoang.ecp.promotion.internal.infrastructure.persistence;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.promotion.internal.application.list.query.PromotionListItem;

/** Maps the JPA entity to the list-promotion application read model. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface PromotionListJpaMapper {
    PromotionListItem listItem(PromotionEntity entity);
}

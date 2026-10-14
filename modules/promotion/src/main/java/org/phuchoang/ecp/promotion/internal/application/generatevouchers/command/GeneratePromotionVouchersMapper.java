package org.phuchoang.ecp.promotion.internal.application.generatevouchers.command;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.promotion.internal.domain.model.Promotion;
import org.phuchoang.ecp.promotion.internal.domain.model.PromotionVoucher;

/** Maps voucher-generation application data to its domain operation and output. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface GeneratePromotionVouchersMapper {

    Promotion.GenerateVouchers domainRequest(GeneratePromotionVouchersCommand command);

    GeneratedVoucher generatedVoucher(PromotionVoucher voucher);
}

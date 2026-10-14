package org.phuchoang.ecp.promotion.api;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.promotion.internal.application.create.command.CreatePromotionCommand;
import org.phuchoang.ecp.promotion.internal.application.create.command.CreatedPromotion;
import org.phuchoang.ecp.promotion.internal.application.generatevouchers.command.GeneratedVoucher;
import org.phuchoang.ecp.promotion.internal.application.list.query.PromotionListItem;
import org.phuchoang.ecp.promotion.internal.application.redeem.command.RedeemPromotionCommand;
import org.phuchoang.ecp.promotion.internal.application.redeem.command.RedeemedPromotion;
import org.phuchoang.ecp.promotion.internal.application.validate.query.VoucherValidation;

import java.math.BigDecimal;
import java.util.UUID;

/** Maps Promotion's published consumer contract to its internal use-case contracts. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface PromotionApiMapper {

    @Mapping(target = "caller", source = "caller")
    @Mapping(target = "correlationId", source = "correlationId")
    @Mapping(target = ".", source = "write")
    CreatePromotionCommand createPromotionCommand(IdentityActor caller, UUID correlationId, PromotionWrite write);

    PromotionView promotionView(CreatedPromotion promotion);

    PromotionView promotionView(PromotionListItem promotion);

    Voucher voucher(GeneratedVoucher voucher);

    VoucherValidationResult voucherValidationResult(VoucherValidation validation);

    RedeemPromotionCommand redeemPromotionCommand(UUID promotionId, UUID orderId, UUID customerId,
            BigDecimal discountableAmount);

    PromotionRedemption promotionRedemption(RedeemedPromotion redemption);
}

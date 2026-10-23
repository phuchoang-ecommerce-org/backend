package org.phuchoang.ecp.promotion.api;

import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.promotion.internal.application.create.command.CreatePromotionService;
import org.phuchoang.ecp.promotion.internal.application.generatevouchers.command.GeneratePromotionVouchersCommand;
import org.phuchoang.ecp.promotion.internal.application.generatevouchers.command.GeneratePromotionVouchersService;
import org.phuchoang.ecp.promotion.internal.application.list.query.ListPromotionsService;
import org.phuchoang.ecp.promotion.internal.application.redeem.command.RedeemPromotionService;
import org.phuchoang.ecp.promotion.internal.application.validate.query.ValidateVoucherService;
import org.phuchoang.ecp.promotion.internal.application.status.command.PromotionStatusResult;
import org.phuchoang.ecp.promotion.internal.application.status.command.SetPromotionStatusCommand;
import org.phuchoang.ecp.promotion.internal.application.status.command.SetPromotionStatusService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Public adapter which maps the module boundary inward without exposing
 * implementation types.
 */
@Component
@lombok.RequiredArgsConstructor(access = lombok.AccessLevel.PACKAGE)
class PromotionFacadeAdapter implements PromotionFacade {
    private final CreatePromotionService createPromotions;
    private final ListPromotionsService listPromotions;
    private final GeneratePromotionVouchersService generateVouchers;
    private final ValidateVoucherService validateVouchers;
    private final RedeemPromotionService redeemPromotions;
    private final SetPromotionStatusService setPromotionStatus;
    private final PromotionApiMapper mapper;

    @Override
    public PromotionView createPromotion(IdentityActor caller, UUID correlationId, PromotionWrite write) {
        return mapper
                .promotionView(createPromotions.create(mapper.createPromotionCommand(caller, correlationId, write)));
    }

    @Override
    public List<PromotionView> listPromotions(IdentityActor caller, String status, int limit) {
        return listPromotions.list(caller, status, limit).stream().map(mapper::promotionView).toList();
    }

    @Override
    public List<Voucher> generatePromotionVouchers(IdentityActor caller, UUID correlationId, UUID promotionId,
            int count, boolean singleUse, String prefix) {
        return generateVouchers.generate(new GeneratePromotionVouchersCommand(caller, correlationId, promotionId, count,
                singleUse, prefix)).stream().map(mapper::voucher).toList();
    }

    @Override
    public PromotionView setPromotionStatus(IdentityActor caller, UUID correlationId, UUID promotionId,
            PromotionStatusWrite write) {
        PromotionStatusResult changed = setPromotionStatus.set(SetPromotionStatusCommand.fromStatusName(caller,
                correlationId, promotionId, write.status(), write.reason(), write.effectiveAt()));
        return promotionView(changed);
    }

    @Override
    public VoucherValidationResult validateVoucher(UUID customerId, String code, BigDecimal discountableAmount) {
        return mapper.voucherValidationResult(validateVouchers.validate(customerId, code, discountableAmount));
    }

    @Override
    public PromotionRedemption redeem(UUID promotionId, UUID orderId, UUID customerId, BigDecimal discountableAmount) {
        try {
            return mapper.promotionRedemption(redeemPromotions.redeem(
                    mapper.redeemPromotionCommand(promotionId, orderId, customerId, discountableAmount)));
        } catch (org.phuchoang.ecp.promotion.internal.application.redeem.command.PromotionUsageLimitReachedException exception) {
            throw new PromotionUsageLimitReachedException();
        }
    }

    private static PromotionView promotionView(PromotionStatusResult value) {
        return new PromotionView(value.id(), value.code(), value.name(), value.description(), value.discountType(),
                value.discountValue(), value.currency(), value.maxDiscountAmount(), value.minOrderAmount(),
                value.validFrom(),
                value.validUntil(), value.usageLimit(), value.usageCount(), value.perCustomerLimit(), value.priority(),
                value.stackable(), value.status());
    }
}

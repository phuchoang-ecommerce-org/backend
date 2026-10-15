package org.phuchoang.ecp.ordering.internal.application.checkout.voucher.command;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.ordering.internal.application.checkout.command.CheckoutCommandFailures;
import org.phuchoang.ecp.ordering.internal.application.checkout.command.CheckoutCommandResult;
import org.phuchoang.ecp.ordering.internal.domain.service.CheckoutDomainService;
import org.phuchoang.ecp.promotion.api.PromotionFacade;
import org.phuchoang.ecp.promotion.api.VoucherValidationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplyCheckoutVoucherService {
    private final IdentityAuthorization authorization;
    private final PromotionFacade promotions;
    private final CheckoutDomainService checkouts;

    @Transactional
    public CheckoutCommandResult apply(IdentityActor caller, UUID checkoutId, String code) {
        authorization.assertAuthorized(caller, "applyCheckoutVoucher");
        VoucherValidationResult validation = promotions.validateVoucher(caller.accountId(), code,
            checkouts.subtotal(checkoutId, caller.accountId()));
        if (!validation.valid()) throw new CheckoutCommandFailures.VoucherNotApplicable();
        return CheckoutCommandResult.from(checkouts.applyVoucher(checkoutId, caller.accountId(), validation.promotionId(),
            validation.code(), validation.discountAmount()), List.of(), false);
    }
}

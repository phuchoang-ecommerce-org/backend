package org.phuchoang.ecp.ordering.internal.application.checkout.voucher.command;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.ordering.internal.domain.service.CheckoutDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RemoveCheckoutVoucherService {
    private final IdentityAuthorization authorization;
    private final CheckoutDomainService checkouts;

    @Transactional
    public void remove(IdentityActor caller, UUID checkoutId) {
        authorization.assertAuthorized(caller, "removeCheckoutVoucher");
        checkouts.removeVoucher(checkoutId, caller.accountId());
    }
}

package org.phuchoang.ecp.ordering.internal.application.checkout.current.query;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.ordering.internal.domain.service.CheckoutDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCurrentCheckoutService {
    private final IdentityAuthorization authorization;
    private final CurrentCheckoutQuery query;

    @Transactional(readOnly = true)
    public CheckoutReadView get(IdentityActor caller) {
        authorization.assertAuthorized(caller, "getCurrentCheckout");
        return query.findDraftByCustomerId(caller.accountId()).orElseThrow(CheckoutDomainService.CheckoutNotFound::new);
    }
}

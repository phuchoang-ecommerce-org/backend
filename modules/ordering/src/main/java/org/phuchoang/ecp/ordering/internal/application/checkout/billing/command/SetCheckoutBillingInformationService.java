package org.phuchoang.ecp.ordering.internal.application.checkout.billing.command;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.identity.api.facade.IdentityFacade;
import org.phuchoang.ecp.ordering.internal.application.checkout.command.CheckoutAddressInput;
import org.phuchoang.ecp.ordering.internal.application.checkout.command.CheckoutCommandResult;
import org.phuchoang.ecp.ordering.internal.domain.model.CheckoutAddress;
import org.phuchoang.ecp.ordering.internal.domain.service.CheckoutDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.phuchoang.ecp.ordering.internal.application.checkout.command.CheckoutCommandSupport.address;

@Service
@RequiredArgsConstructor
public class SetCheckoutBillingInformationService {
    private final IdentityAuthorization authorization;
    private final IdentityFacade identities;
    private final CheckoutDomainService checkouts;

    @Transactional
    public CheckoutCommandResult set(IdentityActor caller, UUID checkoutId, UUID addressId, CheckoutAddressInput input, boolean sameAsShipping) {
        authorization.assertAuthorized(caller, "setCheckoutBillingInformation");
        CheckoutAddress billing = sameAsShipping ? checkouts.shippingAddress(checkoutId, caller.accountId())
            : addressId == null ? address(input) : address(identities.getOwnAddress(caller, addressId));
        if (billing == null) throw new IllegalArgumentException("billing address is required when sameAsShipping is false");
        return CheckoutCommandResult.from(checkouts.setBillingAddress(checkoutId, caller.accountId(), billing), List.of(), false);
    }
}

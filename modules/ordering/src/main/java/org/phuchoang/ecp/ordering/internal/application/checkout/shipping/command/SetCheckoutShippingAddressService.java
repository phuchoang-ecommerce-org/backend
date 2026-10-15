package org.phuchoang.ecp.ordering.internal.application.checkout.shipping.command;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.identity.api.facade.IdentityFacade;
import org.phuchoang.ecp.identity.api.request.AddressWriteRequest;
import org.phuchoang.ecp.ordering.internal.application.checkout.command.CheckoutAddressInput;
import org.phuchoang.ecp.ordering.internal.application.checkout.command.CheckoutCommandResult;
import org.phuchoang.ecp.ordering.internal.domain.model.CheckoutAddress;
import org.phuchoang.ecp.ordering.internal.domain.model.ShippingOption;
import org.phuchoang.ecp.ordering.internal.domain.service.CheckoutDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.phuchoang.ecp.ordering.internal.application.checkout.command.CheckoutCommandSupport.*;

@Service
@RequiredArgsConstructor
public class SetCheckoutShippingAddressService {
    private final IdentityAuthorization authorization;
    private final IdentityFacade identities;
    private final CheckoutDomainService checkouts;
    private final Clock clock;

    @Transactional
    public CheckoutCommandResult set(IdentityActor caller, UUID checkoutId, UUID addressId, CheckoutAddressInput input, boolean saveToAddressBook) {
        authorization.assertAuthorized(caller, "setCheckoutShippingAddress");
        CheckoutAddress address = resolve(caller, addressId, input, saveToAddressBook);
        ShippingOption quote = provisionalQuote(address, checkouts.currency(checkoutId, caller.accountId()), clock.instant());
        return CheckoutCommandResult.from(checkouts.setShippingAddress(checkoutId, caller.accountId(), address, List.of(quote)), List.of(quote), false);
    }

    private CheckoutAddress resolve(IdentityActor caller, UUID addressId, CheckoutAddressInput input, boolean save) {
        if (addressId != null) return address(identities.getOwnAddress(caller, addressId));
        CheckoutAddress value = address(input);
        if (save) {
            return address(identities.addOwnAddress(caller, new AddressWriteRequest(value.label(), value.recipientName(), value.line1(),
                value.line2(), value.city(), value.region(), value.postalCode(), value.countryCode(), value.phone(), false, false)));
        }
        return value;
    }
}

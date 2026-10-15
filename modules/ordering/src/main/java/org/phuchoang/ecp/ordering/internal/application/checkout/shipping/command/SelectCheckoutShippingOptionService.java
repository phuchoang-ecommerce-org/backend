package org.phuchoang.ecp.ordering.internal.application.checkout.shipping.command;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.ordering.internal.application.checkout.command.CheckoutCommandResult;
import org.phuchoang.ecp.ordering.internal.domain.model.CheckoutAddress;
import org.phuchoang.ecp.ordering.internal.domain.model.ShippingOption;
import org.phuchoang.ecp.ordering.internal.domain.service.CheckoutDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.phuchoang.ecp.ordering.internal.application.checkout.command.CheckoutCommandSupport.provisionalQuote;

@Service
@RequiredArgsConstructor
public class SelectCheckoutShippingOptionService {
    private final IdentityAuthorization authorization;
    private final CheckoutDomainService checkouts;
    private final Clock clock;

    @Transactional
    public CheckoutCommandResult select(IdentityActor caller, UUID checkoutId, String optionId) {
        authorization.assertAuthorized(caller, "selectCheckoutShippingOption");
        CheckoutAddress address = checkouts.shippingAddress(checkoutId, caller.accountId());
        if (address == null) throw new IllegalArgumentException("shippingAddress is required before selecting shipping.");
        ShippingOption quote = provisionalQuote(address, checkouts.currency(checkoutId, caller.accountId()), clock.instant());
        if (!quote.id().equals(optionId)) throw new IllegalArgumentException("shippingOptionId is not a current quote.");
        return CheckoutCommandResult.from(checkouts.selectShippingOption(checkoutId, caller.accountId(), quote), List.of(quote), false);
    }
}

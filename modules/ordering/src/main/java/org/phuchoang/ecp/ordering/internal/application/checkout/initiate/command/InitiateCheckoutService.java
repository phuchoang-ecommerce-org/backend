package org.phuchoang.ecp.ordering.internal.application.checkout.initiate.command;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.cart.api.CartFacade;
import org.phuchoang.ecp.cart.api.CartView;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.identity.api.facade.IdentityFacade;
import org.phuchoang.ecp.ordering.internal.application.checkout.command.CheckoutCommandFailures;
import org.phuchoang.ecp.ordering.internal.application.checkout.command.CheckoutCommandResult;
import org.phuchoang.ecp.ordering.internal.domain.model.CheckoutLine;
import org.phuchoang.ecp.ordering.internal.domain.service.CheckoutDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.phuchoang.ecp.ordering.internal.application.checkout.command.CheckoutCommandSupport.*;

/** Starts or resumes the caller's one authoritative Draft checkout. */
@Service
@RequiredArgsConstructor
public class InitiateCheckoutService {
    private final IdentityAuthorization authorization;
    private final IdentityFacade identities;
    private final CartFacade carts;
    private final CheckoutDomainService checkouts;

    @Transactional
    public CheckoutCommandResult initiate(IdentityActor caller) {
        authorization.assertAuthorized(caller, "initiateCheckout");
        if (!"VERIFIED".equals(identities.getOwnAccount(caller).verificationStatus())) {
            throw new CheckoutCommandFailures.UnverifiedAccount();
        }
        CartView cart = carts.getCurrentCart(caller, null);
        List<CheckoutLine> lines = purchasableLines(cart);
        requireEntireCartPurchasable(cart, lines);
        CheckoutDomainService.StartedCheckout started = checkouts.startOrResume(caller.accountId(), lines);
        return CheckoutCommandResult.from(started.checkout(), List.of(), started.resumed());
    }
}

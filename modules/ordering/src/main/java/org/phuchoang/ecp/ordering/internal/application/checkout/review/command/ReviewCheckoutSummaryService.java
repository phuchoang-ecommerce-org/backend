package org.phuchoang.ecp.ordering.internal.application.checkout.review.command;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.cart.api.CartFacade;
import org.phuchoang.ecp.cart.api.CartView;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.ordering.internal.application.checkout.command.CheckoutCommandResult;
import org.phuchoang.ecp.ordering.internal.domain.model.CheckoutLine;
import org.phuchoang.ecp.ordering.internal.domain.service.CheckoutDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.phuchoang.ecp.ordering.internal.application.checkout.command.CheckoutCommandSupport.*;

/** Review refreshes authoritative checkout state and deliberately remains a command despite its GET endpoint. */
@Service
@RequiredArgsConstructor
public class ReviewCheckoutSummaryService {
    private final IdentityAuthorization authorization;
    private final CartFacade carts;
    private final CheckoutDomainService checkouts;

    @Transactional
    public CheckoutSummaryResult review(IdentityActor caller, UUID checkoutId) {
        authorization.assertAuthorized(caller, "getOrderSummary");
        CartView cart = carts.getCurrentCart(caller, null);
        List<CheckoutLine> lines = purchasableLines(cart);
        requireEntireCartPurchasable(cart, lines);
        var before = checkouts.lines(checkoutId, caller.accountId());
        var refreshed = checkouts.refresh(checkoutId, caller.accountId(), lines, UUID.randomUUID().toString());
        return new CheckoutSummaryResult(CheckoutCommandResult.from(refreshed, List.of(), false), !before.equals(lines), List.of(),
            refreshed.confirmationToken());
    }

    public record CheckoutSummaryResult(CheckoutCommandResult checkout, boolean changed, List<SummaryChange> changes,
            String confirmationToken) { public CheckoutSummaryResult { changes = List.copyOf(changes); } }
    public record SummaryChange(String field, String previous, String current) { }
}

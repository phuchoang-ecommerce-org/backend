package org.phuchoang.ecp.ordering.internal.application.checkout.shipping.query;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.ordering.internal.domain.service.CheckoutDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetShippingQuotesService {
    private final IdentityAuthorization authorization;
    private final ShippingQuoteQuery query;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ShippingQuoteResult get(IdentityActor caller, UUID checkoutId) {
        authorization.assertAuthorized(caller, "getShippingQuotes");
        ShippingQuoteContext context = query.findByIdAndCustomerId(checkoutId, caller.accountId())
            .orElseThrow(CheckoutDomainService.CheckoutNotFound::new);
        if (!context.hasShippingAddress()) throw new IllegalArgumentException("shippingAddress is required before quoting shipping.");
        Instant now = clock.instant();
        return new ShippingQuoteResult(List.of(new ShippingQuoteResult.Option("provisional-standard", "PROVISIONAL", "Standard",
            new BigDecimal("5.0000"), context.currency(), now.plus(Duration.ofDays(3)), now.plus(Duration.ofDays(7)))), 1);
    }
}

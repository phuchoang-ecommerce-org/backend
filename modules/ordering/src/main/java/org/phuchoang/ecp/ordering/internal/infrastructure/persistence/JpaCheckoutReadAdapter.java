package org.phuchoang.ecp.ordering.internal.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.ordering.internal.application.checkout.current.query.CheckoutReadView;
import org.phuchoang.ecp.ordering.internal.application.checkout.current.query.CurrentCheckoutQuery;
import org.phuchoang.ecp.ordering.internal.application.checkout.shipping.query.ShippingQuoteContext;
import org.phuchoang.ecp.ordering.internal.application.checkout.shipping.query.ShippingQuoteQuery;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** JPA implementation of checkout read ports; it maps persistence rows directly to application views. */
@Repository
@RequiredArgsConstructor
class JpaCheckoutReadAdapter implements CurrentCheckoutQuery, ShippingQuoteQuery {
    private final CheckoutJpaRepository checkouts;
    private final CheckoutReadJpaMapper mapper;

    @Override
    public Optional<CheckoutReadView> findDraftByCustomerId(UUID customerId) {
        return checkouts.findByCustomerIdAndStatus(customerId, "DRAFT").map(mapper::toReadView);
    }

    @Override
    public Optional<ShippingQuoteContext> findByIdAndCustomerId(UUID checkoutId, UUID customerId) {
        return checkouts.findById(checkoutId).filter(value -> value.getCustomerId().equals(customerId))
            .map(mapper::toShippingQuoteContext);
    }
}

package org.phuchoang.ecp.ordering.internal.application.checkout.shipping.query;

import java.util.Optional;
import java.util.UUID;

/** Read port supplying the data needed for a provisional shipping quote. */
public interface ShippingQuoteQuery {
    Optional<ShippingQuoteContext> findByIdAndCustomerId(UUID checkoutId, UUID customerId);
}

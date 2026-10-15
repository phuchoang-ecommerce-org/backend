package org.phuchoang.ecp.ordering.internal.application.checkout.current.query;

import java.util.Optional;
import java.util.UUID;

/** Read port for the caller's draft checkout; it intentionally bypasses the domain write model. */
public interface CurrentCheckoutQuery {
    Optional<CheckoutReadView> findDraftByCustomerId(UUID customerId);
}

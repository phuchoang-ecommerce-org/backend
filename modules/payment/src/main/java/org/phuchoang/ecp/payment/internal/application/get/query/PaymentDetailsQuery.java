package org.phuchoang.ecp.payment.internal.application.get.query;

import java.util.Optional;
import java.util.UUID;

/** Application read port; the query intentionally bypasses the write model. */
public interface PaymentDetailsQuery {
    Optional<PaymentDetails> findByOrderId(UUID orderId);
}

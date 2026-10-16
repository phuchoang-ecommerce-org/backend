package org.phuchoang.ecp.payment.api;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;

/**
 * Public payment boundary. Provider payloads and credentials are intentionally
 * absent.
 */
public interface PaymentFacade {
  void selectCheckoutPaymentMethod(IdentityActor caller, UUID checkoutId, PaymentMethod method);

  Initiation initiatePayment(IdentityActor caller, UUID orderId, PaymentMethod method, String idempotencyKey,
      URI returnUrl, UUID correlationId);

  PaymentDetails getOrderPayment(UUID orderId);

  CallbackResult receivePaymentProviderNotification(String providerReference, PaymentOutcome outcome, BigDecimal amount,
      String currency, String providerEventId, Instant occurredAt, java.util.Map<String, Object> payload,
      String signature, UUID correlationId);

  void recordOrderCreated(UUID orderId, UUID customerId, BigDecimal totalAmount, String currency, String status,
      Instant occurredAt);

  record Initiation(PaymentDetails payment, PaymentAttemptDetails attempt, URI redirectUrl,
      boolean accepted) {
  }

  record PaymentDetails(UUID id, UUID orderId, UUID customerId, PaymentMethod method, BigDecimal authorisedAmount,
      String currency, String status) {
  }

  record PaymentAttemptDetails(UUID id, int attemptNumber, PaymentOutcome outcome, String providerReference) {
  }

  enum CallbackResult {
    APPLIED, DUPLICATE, UNMATCHED
  }
}

package org.phuchoang.ecp.payment.api;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;

/**
 * Public payment boundary. Provider payloads and credentials are intentionally
 * absent.
 */
public interface PaymentFacade {
  void selectCheckoutPaymentMethod(IdentityActor caller, UUID checkoutId, PaymentMethod method);

  Initiation initiatePayment(IdentityActor caller, UUID orderId, PaymentMethod method, String idempotencyKey,
      URI returnUrl, UUID correlationId);

  PaymentDetails getOrderPayment(IdentityActor caller, UUID orderId);

  UnmatchedPaymentPage listUnmatchedPayments(IdentityActor caller, Boolean resolved, String cursor, int size);

  CallbackResult receivePaymentProviderNotification(String providerReference, PaymentOutcome outcome, BigDecimal amount,
      String currency, String providerEventId, Instant occurredAt, java.util.Map<String, Object> payload,
      String signature, UUID correlationId);

  void recordOrderCreated(UUID orderId, UUID customerId, BigDecimal totalAmount, String currency, String status,
      Instant occurredAt);

  record Initiation(PaymentDetails payment, PaymentAttemptDetails attempt, URI redirectUrl,
      boolean accepted) {
  }

  record PaymentDetails(UUID id, UUID orderId, UUID customerId, PaymentMethod method, BigDecimal authorisedAmount,
      BigDecimal capturedAmount, BigDecimal refundedAmount, String currency, String status, Instant createdAt) {
  }

  record PaymentAttemptDetails(UUID id, UUID paymentId, int attemptNumber, BigDecimal amount, String currency,
      PaymentOutcome outcome, String providerReference, String failureReason, Instant requestedAt, Instant resolvedAt) {
  }

  record UnmatchedPayment(UUID id, String providerReference, BigDecimal amount, String currency, String outcome,
      Instant receivedAt, Instant resolvedAt) { }

  record UnmatchedPaymentPage(List<UnmatchedPayment> items, String nextCursor) {
    public UnmatchedPaymentPage { items = List.copyOf(items); }
  }

  enum CallbackResult {
    APPLIED, DUPLICATE, UNMATCHED
  }
}

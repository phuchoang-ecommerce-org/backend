package org.phuchoang.ecp.payment.internal.domain.service;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;
import org.phuchoang.ecp.payment.internal.domain.model.AttemptOutcome;
import org.phuchoang.ecp.payment.internal.domain.model.Money;
import org.phuchoang.ecp.payment.internal.domain.model.Payment;
import org.phuchoang.ecp.payment.internal.domain.model.PaymentAttempt;
import org.phuchoang.ecp.payment.internal.domain.model.PaymentMethod;
import org.phuchoang.ecp.payment.internal.domain.repository.PaymentRepository;

/** Domain entry point for complete Payment aggregate persistence. */
public final class PaymentDomainService {
  private final PaymentRepository payments;

  public PaymentDomainService(PaymentRepository payments) {
    this.payments = payments;
  }

  public Payment create(UUID orderId, UUID customerId, String method, java.math.BigDecimal amount, String currency,
      Instant at) {
    return Payment.begin(UUID.randomUUID(), orderId, customerId, PaymentMethod.valueOf(method),
        new Money(amount, currency), at).save(payments);
  }

  public Payment requireForOrder(UUID orderId) {
    return Payment.requireForOrder(payments, orderId);
  }

  public Payment requireByProviderReference(String reference) {
    return Payment.requireByProviderReference(payments, reference);
  }

  public PendingAttempt startAttempt(UUID orderId, PaymentMethod method, String idempotencyKey, Instant at) {
    Payment.AttemptStarted started = requireForOrder(orderId).startAttempt(method, idempotencyKey, at);
    return new PendingAttempt(started.created() ? started.payment().save(payments) : started.payment(), started.attempt(),
        started.created());
  }

  public Payment resolveAttempt(UUID orderId, UUID attemptId, AttemptOutcome outcome, BigDecimal amount,
      String providerReference, Instant at) {
    return requireForOrder(orderId).resolve(attemptId, outcome, amount, providerReference, at).save(payments);
  }

  public Payment resolveProviderNotification(String providerReference, AttemptOutcome outcome, BigDecimal amount,
      Instant at) {
    return requireByProviderReference(providerReference)
        .resolveProviderReference(providerReference, outcome, amount, at).save(payments);
  }

  public record PendingAttempt(Payment payment, PaymentAttempt attempt, boolean created) {
  }

}

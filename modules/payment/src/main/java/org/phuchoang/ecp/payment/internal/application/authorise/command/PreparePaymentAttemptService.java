package org.phuchoang.ecp.payment.internal.application.authorise.command;

import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.payment.internal.domain.model.Payment;
import org.phuchoang.ecp.payment.internal.domain.model.PaymentMethod;
import org.phuchoang.ecp.payment.internal.domain.service.PaymentDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The provider boundary is intentionally outside this transaction. Returning
 * from this method commits the durable Pending attempt before any provider can
 * observe its reference, including the dev/test callback simulator.
 */
@Service
@RequiredArgsConstructor
class PreparePaymentAttemptService {
  private final IdentityAuthorization authorization;
  private final PaymentOrderSnapshotPort orders;
  private final PaymentDomainService payments;
  private final Clock clock;

  @Transactional
  PaymentDomainService.PendingAttempt prepare(InitiatePaymentCommand command) {
    requireKey(command.idempotencyKey());
    authorization.assertAuthorized(command.caller(), "initiatePayment");
    PaymentOrderSnapshot order = orders.find(command.orderId()).orElseThrow(Payment.PaymentNotFound::new);
    if (!order.customerId().equals(command.caller().accountId()) || !"PENDING_PAYMENT".equals(order.status()))
      throw new Payment.PaymentNotFound();
    try {
      return payments.startAttempt(command.orderId(), PaymentMethod.valueOf(command.method()), command.idempotencyKey(),
          clock.instant());
    } catch (Payment.IdempotencyKeyReused mismatch) {
      throw new PaymentAttemptRejectedException(PaymentAttemptRejectedException.Reason.IDEMPOTENCY_KEY_REUSED,
          "Idempotency-Key belongs to a different payment request.");
    } catch (Payment.AttemptAlreadyInFlight inFlight) {
      throw new PaymentAttemptRejectedException(PaymentAttemptRejectedException.Reason.ATTEMPT_ALREADY_IN_FLIGHT,
          "Payment is already being checked.");
    }
  }

  private static void requireKey(String value) {
    if (value == null || value.isBlank() || value.length() > 128)
      throw new PaymentAttemptRejectedException(PaymentAttemptRejectedException.Reason.IDEMPOTENCY_KEY_MISSING,
          "Idempotency-Key is required.");
  }
}

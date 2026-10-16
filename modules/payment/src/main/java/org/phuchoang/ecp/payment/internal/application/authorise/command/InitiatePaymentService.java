package org.phuchoang.ecp.payment.internal.application.authorise.command;

import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.payment.internal.domain.model.AttemptOutcome;
import org.phuchoang.ecp.payment.internal.domain.model.Payment;
import org.phuchoang.ecp.payment.internal.domain.model.PaymentMethod;
import org.phuchoang.ecp.payment.internal.domain.service.PaymentDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC-PAY-02: persist the attempt before crossing the provider boundary. */
@Service
@RequiredArgsConstructor
public class InitiatePaymentService {
  private final IdentityAuthorization authorization;
  private final PaymentOrderSnapshotPort orders;
  private final PaymentDomainService payments;
  private final PaymentGateway gateway;
  private final PaymentOutbox outbox;
  private final PaymentCommandMapper mapper;
  private final Clock clock;

  @Transactional
  public InitiatePaymentResult initiate(InitiatePaymentCommand command) {
    validate(command);
    PaymentDomainService.PendingAttempt pending = payments.startAttempt(command.orderId(),
        PaymentMethod.valueOf(command.method()),
        command.idempotencyKey(), clock.instant());
    PaymentGateway.Result result = authorise(command, pending);
    return result.kind() == PaymentGateway.Result.Kind.PENDING ? pendingResult(pending, result)
        : resolvedResult(command, pending, result);
  }

  private void validate(InitiatePaymentCommand command) {
    requireKey(command.idempotencyKey());
    authorization.assertAuthorized(command.caller(), "initiatePayment");
    PaymentOrderSnapshot order = orders.find(command.orderId()).orElseThrow(Payment.PaymentNotFound::new);
    if (!order.customerId().equals(command.caller().accountId()) || !"PENDING_PAYMENT".equals(order.status()))
      throw new Payment.PaymentNotFound();
  }

  private PaymentGateway.Result authorise(InitiatePaymentCommand command, PaymentDomainService.PendingAttempt pending) {
    return gateway.authoriseAndCapture(pending.payment(), pending.attempt(), command.returnUrl());
  }

  private InitiatePaymentResult pendingResult(PaymentDomainService.PendingAttempt pending,
      PaymentGateway.Result result) {
    return mapper.toResult(pending.payment(), pending.attempt(), result.redirectUrl(), true);
  }

  private InitiatePaymentResult resolvedResult(InitiatePaymentCommand command,
      PaymentDomainService.PendingAttempt pending,
      PaymentGateway.Result result) {
    AttemptOutcome outcome = result.kind() == PaymentGateway.Result.Kind.CAPTURED ? AttemptOutcome.SUCCEEDED
        : AttemptOutcome.FAILED;
    Payment resolved = payments.resolveAttempt(command.orderId(), pending.attempt().id(), outcome,
        pending.payment().amount().amount(), result.providerReference(), clock.instant());
    outbox.append(resolved, outcome == AttemptOutcome.SUCCEEDED ? "PaymentCaptured" : "PaymentFailed", command.caller(),
        command.correlationId(), clock.instant());
    return mapper.toResult(resolved, resolved.attempts().getLast(), null, false);
  }

  private static void requireKey(String value) {
    if (value == null || value.isBlank() || value.length() > 128)
      throw new IllegalArgumentException("Idempotency-Key is required.");
  }
}

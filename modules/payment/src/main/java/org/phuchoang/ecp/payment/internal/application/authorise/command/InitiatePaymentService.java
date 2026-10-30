package org.phuchoang.ecp.payment.internal.application.authorise.command;

import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.payment.internal.domain.model.AttemptOutcome;
import org.phuchoang.ecp.payment.internal.domain.model.Payment;
import org.phuchoang.ecp.payment.internal.domain.service.PaymentDomainService;
import org.springframework.stereotype.Service;

/** UC-PAY-02: persist the attempt before crossing the provider boundary. */
@Service
@RequiredArgsConstructor
public class InitiatePaymentService {
  private final PreparePaymentAttemptService preparation;
  private final PaymentDomainService payments;
  private final PaymentGateway gateway;
  private final PaymentOutbox outbox;
  private final PaymentCommandMapper mapper;
  private final Clock clock;

  public InitiatePaymentResult initiate(InitiatePaymentCommand command) {
    PaymentDomainService.PendingAttempt pending = preparation.prepare(command);
    if (!pending.created())
      return mapper.toResult(pending.payment(), pending.attempt(), null, pending.attempt().outcome() == AttemptOutcome.PENDING);
    PaymentGateway.Result result = authorise(command, pending);
    return result.kind() == PaymentGateway.Result.Kind.PENDING ? pendingResult(pending, result)
        : resolvedResult(command, pending, result);
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

}

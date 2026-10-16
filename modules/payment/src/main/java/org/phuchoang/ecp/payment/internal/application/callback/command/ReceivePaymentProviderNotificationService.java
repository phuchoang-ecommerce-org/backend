package org.phuchoang.ecp.payment.internal.application.callback.command;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.payment.internal.application.authorise.command.PaymentOutbox;
import org.phuchoang.ecp.payment.internal.domain.model.Payment;
import org.phuchoang.ecp.payment.internal.domain.model.AttemptOutcome;
import org.phuchoang.ecp.payment.internal.domain.service.PaymentDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReceivePaymentProviderNotificationService {
  private final ProviderNotificationAuthenticator authenticator;
  private final ProviderNotificationStore notifications;
  private final PaymentDomainService payments;
  private final PaymentOutbox outbox;
  private final Clock clock;

  @Transactional
  public Result receive(ProviderNotification notification, String signature, UUID correlationId) {
    authenticate(notification, signature);
    if (duplicate(notification))
      return Result.DUPLICATE;
    return apply(notification, correlationId);
  }

  private void authenticate(ProviderNotification notification, String signature) {
    if (!authenticator.authentic(notification, signature))
      throw new InvalidProviderSignature();
  }

  private boolean duplicate(ProviderNotification notification) {
    return notification.providerEventId() != null && !notifications.claim(notification.providerEventId());
  }

  private Result apply(ProviderNotification notification, UUID correlationId) {
    try {
      Payment payment = payments.resolveProviderNotification(notification.providerReference(),
          AttemptOutcome.valueOf(notification.outcome()),
          notification.amount(), resolvedAt(notification));
      appendOutcome(payment, notification, correlationId);
      return Result.APPLIED;
    } catch (Payment.PaymentNotFound missing) {
      notifications.recordUnmatched(notification);
      return Result.UNMATCHED;
    }
  }

  private Instant resolvedAt(ProviderNotification notification) {
    return notification.occurredAt() == null ? clock.instant() : notification.occurredAt();
  }

  private void appendOutcome(Payment payment, ProviderNotification notification, UUID correlationId) {
    outbox.append(payment, notification.outcome().equals("SUCCEEDED") ? "PaymentCaptured" : "PaymentFailed",
        null, correlationId, clock.instant());
  }

  public enum Result {
    APPLIED, DUPLICATE, UNMATCHED
  }

  public static final class InvalidProviderSignature extends RuntimeException {
  }
}

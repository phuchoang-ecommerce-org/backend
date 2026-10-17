package org.phuchoang.ecp.payment.internal.application.callback.command;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.phuchoang.ecp.payment.internal.application.authorise.command.PaymentOutbox;
import org.phuchoang.ecp.payment.internal.domain.model.AttemptOutcome;
import org.phuchoang.ecp.payment.internal.domain.model.Payment;
import org.phuchoang.ecp.payment.internal.domain.service.PaymentDomainService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ReceivePaymentProviderNotificationServiceTest {
  @ParameterizedTest
  @MethodSource("unreconcilableOutcomes")
  void providerDiscrepanciesAreRetainedForReconciliation(RuntimeException discrepancy) {
    ProviderNotificationAuthenticator authenticator = mock(ProviderNotificationAuthenticator.class);
    ProviderNotificationStore store = mock(ProviderNotificationStore.class);
    PaymentDomainService payments = mock(PaymentDomainService.class);
    PaymentOutbox outbox = mock(PaymentOutbox.class);
    Instant at = Instant.parse("2026-10-17T00:00:00Z");
    ProviderNotification notification = new ProviderNotification("ref", "SUCCEEDED", new BigDecimal("10.00"),
        "USD", "event", at, Map.of());
    when(authenticator.authentic(notification, "signature")).thenReturn(true);
    when(store.claim("event")).thenReturn(true);
    when(payments.resolveProviderNotification("ref", AttemptOutcome.SUCCEEDED, notification.amount(), at))
        .thenThrow(discrepancy);
    var service = new ReceivePaymentProviderNotificationService(authenticator, store, payments, outbox,
        Clock.fixed(at, ZoneOffset.UTC));

    assertThat(service.receive(notification, "signature", UUID.randomUUID()))
        .isEqualTo(ReceivePaymentProviderNotificationService.Result.UNMATCHED);
    verify(store).recordUnmatched(notification);
    verifyNoInteractions(outbox);
  }

  static java.util.stream.Stream<RuntimeException> unreconcilableOutcomes() {
    return java.util.stream.Stream.of(new Payment.ContradictoryResult(), new Payment.AmountMismatch());
  }
}

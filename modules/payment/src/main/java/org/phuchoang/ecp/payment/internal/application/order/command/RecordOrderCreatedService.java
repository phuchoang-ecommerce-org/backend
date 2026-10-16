package org.phuchoang.ecp.payment.internal.application.order.command;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.payment.internal.application.authorise.command.PaymentOrderSnapshot;
import org.phuchoang.ecp.payment.internal.application.selection.command.CheckoutPaymentSelectionPort;
import org.phuchoang.ecp.payment.internal.domain.model.Payment;
import org.phuchoang.ecp.payment.internal.domain.service.PaymentDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecordOrderCreatedService {
  private final OrderCreatedSnapshotStore snapshots;
  private final CheckoutPaymentSelectionPort selections;
  private final PaymentDomainService payments;

  @Transactional
  public void record(UUID orderId, UUID customerId, BigDecimal total, String currency, String status,
      Instant occurredAt) {
    snapshots.save(new PaymentOrderSnapshot(orderId, customerId, total, currency, status));
    try {
      payments.requireForOrder(orderId);
      return;
    } catch (Payment.PaymentNotFound ignored) {
    }
    String method = selections.findMethod(orderId).map(Enum::name).orElse("CARD");
    payments.create(orderId, customerId, method, total, currency, occurredAt);
  }
}

package org.phuchoang.ecp.payment.internal.application.get.query;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetOrderPaymentService {
  private final PaymentDetailsQuery payments;

  public PaymentDetails get(UUID orderId) {
    return payments.findByOrderId(orderId).orElseThrow(PaymentNotFound::new);
  }

  public static final class PaymentNotFound extends RuntimeException {
  }
}

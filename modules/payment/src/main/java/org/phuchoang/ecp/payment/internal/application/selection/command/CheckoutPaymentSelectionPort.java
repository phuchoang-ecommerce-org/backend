package org.phuchoang.ecp.payment.internal.application.selection.command;

import java.util.Optional;
import java.util.UUID;
import org.phuchoang.ecp.payment.internal.domain.model.PaymentMethod;

public interface CheckoutPaymentSelectionPort {
  void save(UUID checkoutId, UUID customerId, PaymentMethod method);

  Optional<PaymentMethod> findMethod(UUID checkoutId);
}

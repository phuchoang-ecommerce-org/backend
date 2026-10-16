package org.phuchoang.ecp.payment.internal.domain.repository;

import java.util.Optional;
import java.util.UUID;
import org.jmolecules.ddd.annotation.Repository;
import org.phuchoang.ecp.payment.internal.domain.model.Payment;

@Repository
public interface PaymentRepository {
  Optional<Payment> findByOrderId(UUID orderId);

  Optional<Payment> findByProviderReference(String providerReference);

  Payment save(Payment payment);
}

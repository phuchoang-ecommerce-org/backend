package org.phuchoang.ecp.payment.internal.application.authorise.command;

import java.util.Optional;
import java.util.UUID;

public interface PaymentOrderSnapshotPort {
  Optional<PaymentOrderSnapshot> find(UUID orderId);
}

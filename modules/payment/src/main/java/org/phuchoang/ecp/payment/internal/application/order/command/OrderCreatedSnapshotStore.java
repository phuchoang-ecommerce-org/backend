package org.phuchoang.ecp.payment.internal.application.order.command;

import org.phuchoang.ecp.payment.internal.application.authorise.command.PaymentOrderSnapshot;

public interface OrderCreatedSnapshotStore {
  void save(PaymentOrderSnapshot snapshot);
}

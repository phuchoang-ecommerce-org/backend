package org.phuchoang.ecp.payment.internal.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.payment.internal.application.authorise.command.PaymentOrderSnapshot;
import org.phuchoang.ecp.payment.internal.application.authorise.command.PaymentOrderSnapshotPort;
import org.phuchoang.ecp.payment.internal.application.order.command.OrderCreatedSnapshotStore;
import org.phuchoang.ecp.payment.internal.application.selection.command.CheckoutPaymentSelectionPort;
import org.phuchoang.ecp.payment.internal.domain.model.PaymentMethod;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JdbcPaymentOrderSnapshotAdapter
    implements PaymentOrderSnapshotPort, OrderCreatedSnapshotStore, CheckoutPaymentSelectionPort {
  private final JdbcClient jdbc;

  @Override
  public Optional<PaymentOrderSnapshot> find(UUID id) {
    return jdbc.sql("SELECT * FROM payment_order_snapshot WHERE order_id=:id").param("id", id)
        .query((rs, row) -> new PaymentOrderSnapshot(rs.getObject("order_id", UUID.class),
            rs.getObject("customer_id", UUID.class), rs.getBigDecimal("total_amount"), rs.getString("currency"),
            rs.getString("status")))
        .optional();
  }

  @Override
  public void save(PaymentOrderSnapshot s) {
    jdbc.sql(
        "INSERT INTO payment_order_snapshot (order_id,customer_id,total_amount,currency,status) VALUES (:id,:customer,:total,:currency,:status) ON CONFLICT (order_id) DO UPDATE SET status=EXCLUDED.status,updated_at=now()")
        .param("id", s.orderId()).param("customer", s.customerId()).param("total", s.totalAmount())
        .param("currency", s.currency()).param("status", s.status()).update();
  }

  @Override
  public void save(UUID id, UUID customer, PaymentMethod method) {
    jdbc.sql(
        "INSERT INTO payment_checkout_selection (checkout_id,customer_id,method) VALUES (:id,:customer,:method) ON CONFLICT (checkout_id) DO UPDATE SET method=EXCLUDED.method,selected_at=now()")
        .param("id", id).param("customer", customer).param("method", method.name()).update();
  }

  @Override
  public Optional<PaymentMethod> findMethod(UUID id) {
    return jdbc.sql("SELECT method FROM payment_checkout_selection WHERE checkout_id=:id").param("id", id)
        .query((rs, row) -> PaymentMethod.valueOf(rs.getString(1))).optional();
  }
}

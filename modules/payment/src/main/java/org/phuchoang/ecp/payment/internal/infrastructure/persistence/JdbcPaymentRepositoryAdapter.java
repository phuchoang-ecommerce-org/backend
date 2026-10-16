package org.phuchoang.ecp.payment.internal.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.payment.internal.domain.model.AttemptOutcome;
import org.phuchoang.ecp.payment.internal.domain.model.Payment;
import org.phuchoang.ecp.payment.internal.domain.model.PaymentAttempt;
import org.phuchoang.ecp.payment.internal.domain.model.PaymentMethod;
import org.phuchoang.ecp.payment.internal.domain.model.PaymentStatus;
import org.phuchoang.ecp.payment.internal.domain.repository.PaymentRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * PostgreSQL adapter for the complete Payment aggregate and its owned attempts.
 */
@Repository
@RequiredArgsConstructor
class JdbcPaymentRepositoryAdapter implements PaymentRepository {
  private final JdbcClient jdbc;
  private final PaymentPersistenceMapper mapper;

  @Override
  public Optional<Payment> findByOrderId(UUID orderId) {
    return find("order_id", orderId);
  }

  @Override
  public Optional<Payment> findByProviderReference(String reference) {
    return jdbc.sql(
        "SELECT p.* FROM payment_payment p JOIN payment_attempt a ON a.payment_id = p.id WHERE a.provider_reference = :reference")
        .param("reference", reference).query(this::payment).optional();
  }

  private Optional<Payment> find(String column, UUID value) {
    return jdbc.sql("SELECT * FROM payment_payment WHERE " + column + " = :value").param("value", value)
        .query(this::payment).optional();
  }

  private Payment payment(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
    PaymentRow payment = new PaymentRow(rs.getObject("id", UUID.class), rs.getObject("order_id", UUID.class),
        rs.getObject("customer_id", UUID.class), PaymentMethod.valueOf(rs.getString("method")),
        rs.getBigDecimal("authorised_amount"), rs.getString("currency"), PaymentStatus.valueOf(rs.getString("status")),
        rs.getTimestamp("created_at").toInstant(), rs.getLong("version"));
    List<PaymentAttempt> attempts = jdbc
        .sql("SELECT * FROM payment_attempt WHERE payment_id = :id ORDER BY attempt_number").param("id", payment.id())
        .query((attempt, ignored) -> mapper.toDomain(new PaymentAttemptRow(attempt.getObject("id", UUID.class),
            attempt.getObject("payment_id", UUID.class), attempt.getString("idempotency_key"),
            attempt.getInt("attempt_number"), attempt.getBigDecimal("amount"), attempt.getString("currency"),
            AttemptOutcome.valueOf(attempt.getString("outcome")), attempt.getString("provider_reference"),
            attempt.getString("failure_reason"), attempt.getTimestamp("requested_at").toInstant(),
            attempt.getTimestamp("resolved_at") == null ? null : attempt.getTimestamp("resolved_at").toInstant())))
        .list();
    return mapper.toDomain(payment, attempts);
  }

  @Override
  public Payment save(Payment payment) {
    // The existing schema has no separate requested-total column.
    // `authorised_amount` is the
    // immutable order amount for this aggregate; captured remains zero until a
    // success fact.
    PaymentRow row = mapper.toRow(payment);
    var authorised = row.authorisedAmount();
    var captured = payment.status() == PaymentStatus.CAPTURED ? authorised : java.math.BigDecimal.ZERO;
    jdbc.sql(
        """
            INSERT INTO payment_payment (id, order_id, customer_id, method, currency, authorised_amount, captured_amount, status, created_at, updated_at, version)
            VALUES (:id,:orderId,:customerId,:method,:currency,:authorised,:captured,:status,:createdAt,now(),0)
            ON CONFLICT (id) DO UPDATE SET method = EXCLUDED.method, authorised_amount = EXCLUDED.authorised_amount,
              captured_amount = EXCLUDED.captured_amount, status = EXCLUDED.status, updated_at = now(), version = payment_payment.version + 1
            """)
        .param("id", row.id()).param("orderId", row.orderId()).param("customerId", row.customerId())
        .param("method", row.method().name())
        .param("currency", row.currency()).param("authorised", authorised).param("captured", captured)
        .param("status", row.status().name()).param("createdAt", row.createdAt()).update();
    for (PaymentAttempt attempt : payment.attempts()) {
      PaymentAttemptRow attemptRow = mapper.toRow(attempt, payment.id());
      jdbc.sql(
          """
              INSERT INTO payment_attempt (id,payment_id,idempotency_key,attempt_number,amount,currency,outcome,provider_reference,failure_reason,requested_at,resolved_at,created_at,updated_at)
              VALUES (:id,:paymentId,:key,:number,:amount,:currency,:outcome,:reference,:reason,:requested,:resolved,now(),now())
              ON CONFLICT (id) DO UPDATE SET outcome=EXCLUDED.outcome, provider_reference=EXCLUDED.provider_reference,
                failure_reason=EXCLUDED.failure_reason, resolved_at=EXCLUDED.resolved_at, updated_at=now()
              """)
          .param("id", attemptRow.id()).param("paymentId", attemptRow.paymentId())
          .param("key", attemptRow.idempotencyKey())
          .param("number", attemptRow.attemptNumber())
          .param("amount", attemptRow.amount()).param("currency", attemptRow.currency())
          .param("outcome", attemptRow.outcome().name())
          .param("reference", attemptRow.providerReference()).param("reason", attemptRow.failureReason())
          .param("requested", attemptRow.requestedAt()).param("resolved", attemptRow.resolvedAt()).update();
    }
    return new Payment(payment.id(), payment.orderId(), payment.customerId(), payment.method(), payment.amount(),
        payment.status(), payment.attempts(), payment.createdAt(), payment.version() + 1);
  }
}

package org.phuchoang.ecp.payment.internal.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.payment.internal.application.authorise.command.PaymentOutbox;
import org.phuchoang.ecp.payment.internal.domain.model.Payment;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JdbcPaymentOutbox implements PaymentOutbox {
  private final JdbcClient jdbc;

  @Override
  public void append(Payment payment, String type, IdentityActor actor, UUID correlation, Instant occurred) {
    jdbc.sql(
        "INSERT INTO payment_outbox (event_id,event_type,event_version,occurred_at,aggregate_type,aggregate_id,correlation_id,actor_user_id,actor_role,payload,topic) VALUES (:id,:type,1,:occurred,'Payment',:payment,:correlation,:actor,:role,CAST(:payload AS jsonb),'ecp.payment.payment.v1')")
        .param("id", UUID.randomUUID()).param("type", type).param("occurred", occurred).param("payment", payment.id())
        .param("correlation", correlation)
        .param("actor", actor == null ? null : actor.accountId())
        .param("role", actor == null ? null : actor.roles().stream().sorted().findFirst().orElse(null))
        .param("payload", "{\"paymentId\":\"" + payment.id() + "\",\"orderId\":\"" + payment.orderId()
            + "\",\"status\":\"" + payment.status() + "\"}")
        .update();
  }
}

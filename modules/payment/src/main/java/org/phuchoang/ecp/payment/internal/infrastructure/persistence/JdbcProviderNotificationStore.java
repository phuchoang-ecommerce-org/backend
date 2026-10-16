package org.phuchoang.ecp.payment.internal.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.payment.internal.application.callback.command.ProviderNotification;
import org.phuchoang.ecp.payment.internal.application.callback.command.ProviderNotificationStore;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JdbcProviderNotificationStore implements ProviderNotificationStore {
  private final JdbcClient jdbc;

  @Override
  public boolean claim(String id) {
    return jdbc
        .sql("INSERT INTO payment_processed_provider_event (provider_event_id) VALUES (:id) ON CONFLICT DO NOTHING")
        .param("id", id).update() == 1;
  }

  @Override
  public void recordUnmatched(ProviderNotification n) {
    jdbc.sql(
        """
            INSERT INTO payment_unmatched_notification (id,provider_event_id,provider_reference,outcome,amount,currency,received_at,payload)
            VALUES (:id,:event,:reference,:outcome,:amount,:currency,:received,CAST('{}' AS jsonb)) ON CONFLICT (provider_event_id) DO NOTHING
            """)
        .param("id", UUID.randomUUID()).param("event", n.providerEventId()).param("reference", n.providerReference())
        .param("outcome", n.outcome())
        .param("amount", n.amount()).param("currency", n.currency())
        .param("received", n.occurredAt() == null ? Instant.now() : n.occurredAt()).update();
  }
}

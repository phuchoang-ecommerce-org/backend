package org.phuchoang.ecp.payment.internal.infrastructure.persistence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.phuchoang.ecp.payment.internal.application.reconciliation.query.UnmatchedPaymentPage;
import org.phuchoang.ecp.payment.internal.application.reconciliation.query.UnmatchedPaymentsQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Keyset read of reconciliation evidence; no payment aggregate is loaded. */
@Component
class JdbcUnmatchedPaymentsQuery implements UnmatchedPaymentsQuery {
  private final JdbcClient jdbc;
  private final String activeId;
  private final String activeKey;
  private final String previousId;
  private final String previousKey;

  JdbcUnmatchedPaymentsQuery(JdbcClient jdbc, @Value("${ecp.cursor.active-key-id:cursor-v1}") String activeId,
      @Value("${ecp.cursor.active-key:}") String activeKey,
      @Value("${ecp.cursor.previous-key-id:}") String previousId,
      @Value("${ecp.cursor.previous-key:}") String previousKey) {
    if (activeId.isBlank() || activeId.contains("|") || activeKey.getBytes(StandardCharsets.UTF_8).length < 32
        || previousId.isBlank() != previousKey.isBlank() || previousId.contains("|")
        || (!previousId.isBlank() && (previousId.equals(activeId)
            || previousKey.getBytes(StandardCharsets.UTF_8).length < 32))) {
      throw new IllegalStateException("Invalid cursor signing key configuration.");
    }
    this.jdbc = jdbc;
    this.activeId = activeId;
    this.activeKey = activeKey;
    this.previousId = previousId;
    this.previousKey = previousKey;
  }

  @Override
  public UnmatchedPaymentPage list(Boolean resolved, String cursor, int size) {
    Position position = cursor == null ? null : decode(cursor, resolved);
    String sql = "SELECT id, provider_reference, amount, currency, outcome, received_at, resolved_at "
        + "FROM payment_unmatched_notification WHERE 1=1"
        + (resolved == null ? "" : resolved ? " AND resolved_at IS NOT NULL" : " AND resolved_at IS NULL")
        + (position == null ? "" : " AND (received_at, id) < (:received, :id)")
        + " ORDER BY received_at DESC, id DESC LIMIT :limit";
    JdbcClient.StatementSpec statement = jdbc.sql(sql).param("limit", size + 1);
    if (position != null)
      statement = statement.param("received", Timestamp.from(position.receivedAt())).param("id", position.id());
    List<UnmatchedPaymentPage.Item> rows = statement.query((rs, ignored) -> new UnmatchedPaymentPage.Item(
        rs.getObject("id", UUID.class), rs.getString("provider_reference"), rs.getBigDecimal("amount"),
        rs.getString("currency"), rs.getString("outcome"), rs.getTimestamp("received_at").toInstant(),
        rs.getTimestamp("resolved_at") == null ? null : rs.getTimestamp("resolved_at").toInstant())).list();
    boolean more = rows.size() > size;
    List<UnmatchedPaymentPage.Item> page = more ? rows.subList(0, size) : rows;
    String next = more ? encode(page.getLast(), resolved) : null;
    return new UnmatchedPaymentPage(page, next);
  }

  private String encode(UnmatchedPaymentPage.Item item, Boolean resolved) {
    String payload = String.join("|", "v1", activeId, filter(resolved), item.receivedAt().toString(),
        item.id().toString());
    String body = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    return body + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sign(body, activeKey));
  }

  private Position decode(String cursor, Boolean resolved) {
    try {
      String[] token = cursor.split("\\.", -1);
      if (token.length != 2)
        throw new IllegalArgumentException();
      String[] fields = new String(Base64.getUrlDecoder().decode(token[0]), StandardCharsets.UTF_8).split("\\|", -1);
      if (fields.length != 5 || !fields[0].equals("v1") || !fields[2].equals(filter(resolved)))
        throw new IllegalArgumentException();
      String key = fields[1].equals(activeId) ? activeKey
          : !previousId.isBlank() && fields[1].equals(previousId) ? previousKey : null;
      if (key == null || !MessageDigest.isEqual(Base64.getUrlDecoder().decode(token[1]), sign(token[0], key)))
        throw new IllegalArgumentException();
      return new Position(Instant.parse(fields[3]), UUID.fromString(fields[4]));
    } catch (RuntimeException invalid) {
      throw new IllegalArgumentException("Invalid cursor.");
    }
  }

  private static byte[] sign(String payload, String key) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
    } catch (java.security.GeneralSecurityException failure) {
      throw new IllegalStateException("Cannot sign cursor.", failure);
    }
  }

  private static String filter(Boolean resolved) {
    return resolved == null ? "all" : resolved.toString();
  }

  private record Position(Instant receivedAt, UUID id) {
  }
}

package org.phuchoang.ecp.payment.internal.application.reconciliation.query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UnmatchedPaymentPage(List<Item> items, String nextCursor) {
  public UnmatchedPaymentPage { items = List.copyOf(items); }
  public record Item(UUID id, String providerReference, BigDecimal amount, String currency, String outcome,
      Instant receivedAt, Instant resolvedAt) { }
}

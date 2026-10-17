package org.phuchoang.ecp.payment.internal.application.reconciliation.query;

/** Observational query over the payment reconciliation queue. */
public interface UnmatchedPaymentsQuery {
  UnmatchedPaymentPage list(Boolean resolved, String cursor, int size);
}

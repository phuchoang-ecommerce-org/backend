package org.phuchoang.ecp.payment.internal.application.authorise.command;

/** Application outcome that the public facade translates to the payment error contract. */
public final class PaymentAttemptRejectedException extends RuntimeException {
  public enum Reason {
    IDEMPOTENCY_KEY_MISSING,
    IDEMPOTENCY_KEY_REUSED,
    ATTEMPT_ALREADY_IN_FLIGHT
  }

  private final Reason reason;

  public PaymentAttemptRejectedException(Reason reason, String message) {
    super(message);
    this.reason = reason;
  }

  public Reason reason() {
    return reason;
  }
}

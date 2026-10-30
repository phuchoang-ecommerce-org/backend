package org.phuchoang.ecp.payment.api;

/** Public, safe-to-render payment failure with a stable contract code. */
public final class PaymentException extends RuntimeException {
  private final PaymentErrorCode errorCode;

  public PaymentException(PaymentErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public PaymentErrorCode errorCode() {
    return errorCode;
  }
}

package org.phuchoang.ecp.payment.internal.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.phuchoang.ecp.payment.internal.domain.repository.PaymentRepository;

/** Payment aggregate: provider results are facts and are never overwritten. */
@AggregateRoot
public record Payment(@Identity UUID id, UUID orderId, UUID customerId, PaymentMethod method, Money amount,
    PaymentStatus status, List<PaymentAttempt> attempts, Instant createdAt, long version) {
  public Payment {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(orderId, "orderId");
    Objects.requireNonNull(customerId, "customerId");
    Objects.requireNonNull(method, "method");
    Objects.requireNonNull(amount, "amount");
    Objects.requireNonNull(status, "status");
    attempts = List.copyOf(attempts);
    Objects.requireNonNull(createdAt, "createdAt");
  }

  public static Payment begin(UUID id, UUID orderId, UUID customerId, PaymentMethod method, Money amount,
      Instant createdAt) {
    if (amount.amount().signum() <= 0)
      throw new IllegalArgumentException("A payment must have a positive amount.");
    return new Payment(id, orderId, customerId, method, amount, PaymentStatus.PENDING, List.of(), createdAt, 0);
  }

  /**
   * Aggregate-owned lookup: callers receive only a complete authoritative root.
   */
  public static Payment requireForOrder(PaymentRepository repository, UUID orderId) {
    return repository.findByOrderId(orderId).orElseThrow(PaymentNotFound::new);
  }

  /**
   * Aggregate-owned lookup used only to apply a provider fact to its owning root.
   */
  public static Payment requireByProviderReference(PaymentRepository repository, String providerReference) {
    return repository.findByProviderReference(providerReference).orElseThrow(PaymentNotFound::new);
  }

  public PaymentAttempt startAttempt(String idempotencyKey, Instant at) {
    if (method == PaymentMethod.CASH_ON_DELIVERY)
      throw new IllegalStateException("Cash on delivery is not authorised online.");
    if (attempts.stream().anyMatch(value -> value.outcome() == AttemptOutcome.PENDING))
      throw new AttemptAlreadyInFlight();
    return PaymentAttempt.pending(idempotencyKey, attempts.size() + 1, amount, at);
  }

  public Payment withAttempt(PaymentAttempt attempt) {
    List<PaymentAttempt> changed = new ArrayList<>(attempts);
    changed.add(attempt);
    return new Payment(id, orderId, customerId, method, amount, status, changed, createdAt, version);
  }

  public AttemptStarted startAttempt(PaymentMethod requestedMethod, String idempotencyKey, Instant at) {
    var prior = attempts.stream().filter(value -> value.idempotencyKey().equals(idempotencyKey)).findFirst();
    if (prior.isPresent()) {
      if (method != requestedMethod)
        throw new IdempotencyKeyReused();
      return new AttemptStarted(this, prior.get(), false);
    }
    if (method != requestedMethod)
      throw new PaymentMethodMismatch();
    PaymentAttempt attempt = startAttempt(idempotencyKey, at);
    return new AttemptStarted(withAttempt(attempt), attempt, true);
  }

  public Payment resolve(UUID attemptId, AttemptOutcome outcome, BigDecimal receivedAmount, String providerReference,
      Instant at) {
    PaymentAttempt current = attempts.stream().filter(value -> value.id().equals(attemptId)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Attempt does not belong to payment."));
    if (current.outcome() != AttemptOutcome.PENDING) {
      if (current.same(outcome, receivedAmount, providerReference))
        return this;
      throw new ContradictoryResult();
    }
    if (receivedAmount == null || amount.amount().compareTo(receivedAmount) != 0)
      throw new AmountMismatch();
    List<PaymentAttempt> changed = attempts.stream().map(value -> value.id().equals(attemptId)
        ? value.resolve(outcome, providerReference, at)
        : value).toList();
    PaymentStatus next = outcome == AttemptOutcome.SUCCEEDED ? PaymentStatus.CAPTURED : PaymentStatus.FAILED;
    return new Payment(id, orderId, customerId, method, amount, next, changed, createdAt, version);
  }

  public Payment resolveProviderReference(String providerReference, AttemptOutcome outcome, BigDecimal receivedAmount,
      Instant at) {
    PaymentAttempt attempt = attempts.stream()
        .filter(value -> providerReference.equals(value.providerReference()))
        .findFirst().orElseThrow(PaymentNotFound::new);
    return resolve(attempt.id(), outcome, receivedAmount, providerReference, at);
  }

  /** The aggregate is the sole owner of its complete persistence operation. */
  public Payment save(PaymentRepository repository) {
    return repository.save(this);
  }

  public record AttemptStarted(Payment payment, PaymentAttempt attempt, boolean created) {
  }

  public static final class AttemptAlreadyInFlight extends RuntimeException {
  }

  public static final class PaymentMethodMismatch extends RuntimeException {
  }

  public static final class IdempotencyKeyReused extends RuntimeException {
  }

  public static final class ContradictoryResult extends RuntimeException {
  }

  public static final class AmountMismatch extends RuntimeException {
  }

  public static final class PaymentNotFound extends RuntimeException {
  }
}

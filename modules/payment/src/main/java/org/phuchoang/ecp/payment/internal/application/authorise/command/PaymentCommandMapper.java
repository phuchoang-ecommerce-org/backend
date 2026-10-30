package org.phuchoang.ecp.payment.internal.application.authorise.command;

import java.net.URI;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.payment.internal.domain.model.Payment;
import org.phuchoang.ecp.payment.internal.domain.model.PaymentAttempt;

/** Mechanical domain-to-application mapping for the authorisation use case. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PaymentCommandMapper {

  @Mapping(target = "paymentId", source = "payment.id")
  @Mapping(target = "orderId", source = "payment.orderId")
  @Mapping(target = "customerId", source = "payment.customerId")
  @Mapping(target = "method", source = "payment.method")
  @Mapping(target = "authorisedAmount", source = "payment.amount.amount")
  @Mapping(target = "currency", source = "payment.amount.currency")
  @Mapping(target = "status", source = "payment.status")
  @Mapping(target = "attemptId", source = "attempt.id")
  @Mapping(target = "attemptNumber", source = "attempt.attemptNumber")
  @Mapping(target = "outcome", source = "attempt.outcome")
  @Mapping(target = "providerReference", source = "attempt.providerReference")
  @Mapping(target = "requestedAt", source = "attempt.requestedAt")
  @Mapping(target = "resolvedAt", source = "attempt.resolvedAt")
  InitiatePaymentResult toResult(Payment payment, PaymentAttempt attempt, URI redirectUrl, boolean accepted);
}

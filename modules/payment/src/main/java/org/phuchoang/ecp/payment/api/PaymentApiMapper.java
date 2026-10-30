package org.phuchoang.ecp.payment.api;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.payment.internal.application.authorise.command.InitiatePaymentResult;

/**
 * Maps private application result/read models onto the stable public contract.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface PaymentApiMapper {

  @Mapping(target = "payment.id", source = "paymentId")
  @Mapping(target = "payment.orderId", source = "orderId")
  @Mapping(target = "payment.customerId", source = "customerId")
  @Mapping(target = "payment.method", source = "method")
  @Mapping(target = "payment.authorisedAmount", source = "authorisedAmount")
  @Mapping(target = "payment.currency", source = "currency")
  @Mapping(target = "payment.status", source = "status")
  @Mapping(target = "attempt.id", source = "attemptId")
  @Mapping(target = "attempt.paymentId", source = "paymentId")
  @Mapping(target = "attempt.attemptNumber", source = "attemptNumber")
  @Mapping(target = "attempt.amount", source = "authorisedAmount")
  @Mapping(target = "attempt.currency", source = "currency")
  @Mapping(target = "attempt.outcome", source = "outcome")
  @Mapping(target = "attempt.providerReference", source = "providerReference")
  @Mapping(target = "attempt.failureReason", ignore = true)
  @Mapping(target = "attempt.requestedAt", source = "requestedAt")
  @Mapping(target = "attempt.resolvedAt", source = "resolvedAt")
  PaymentFacade.Initiation toInitiation(InitiatePaymentResult result);

  PaymentFacade.PaymentDetails toPaymentDetails(
      org.phuchoang.ecp.payment.internal.application.get.query.PaymentDetails result);
}

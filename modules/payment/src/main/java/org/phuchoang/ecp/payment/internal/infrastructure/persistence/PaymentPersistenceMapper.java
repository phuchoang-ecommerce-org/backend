package org.phuchoang.ecp.payment.internal.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.payment.internal.domain.model.Money;
import org.phuchoang.ecp.payment.internal.domain.model.Payment;
import org.phuchoang.ecp.payment.internal.domain.model.PaymentAttempt;

/**
 * MapStruct boundary between local JDBC rows and the technology-free Payment
 * model.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR, imports = Money.class)
interface PaymentPersistenceMapper {

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "id", source = "row.id")
  @Mapping(target = "orderId", source = "row.orderId")
  @Mapping(target = "customerId", source = "row.customerId")
  @Mapping(target = "method", source = "row.method")
  @Mapping(target = "amount", expression = "java(new Money(row.authorisedAmount(), row.currency()))")
  @Mapping(target = "status", source = "row.status")
  @Mapping(target = "attempts", source = "attempts")
  @Mapping(target = "createdAt", source = "row.createdAt")
  @Mapping(target = "version", source = "row.version")
  Payment toDomain(PaymentRow row, List<PaymentAttempt> attempts);

  @Mapping(target = "amount", expression = "java(new Money(row.amount(), row.currency()))")
  PaymentAttempt toDomain(PaymentAttemptRow row);

  @Mapping(target = "authorisedAmount", source = "amount.amount")
  @Mapping(target = "currency", source = "amount.currency")
  PaymentRow toRow(Payment payment);

  @Mapping(target = "paymentId", source = "paymentId")
  @Mapping(target = "amount", source = "attempt.amount.amount")
  @Mapping(target = "currency", source = "attempt.amount.currency")
  PaymentAttemptRow toRow(PaymentAttempt attempt, UUID paymentId);
}

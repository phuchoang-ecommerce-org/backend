package org.phuchoang.ecp.payment.api;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.payment.internal.application.authorise.command.InitiatePaymentCommand;
import org.phuchoang.ecp.payment.internal.application.authorise.command.InitiatePaymentService;
import org.phuchoang.ecp.payment.internal.application.callback.command.ProviderNotification;
import org.phuchoang.ecp.payment.internal.application.callback.command.ReceivePaymentProviderNotificationService;
import org.phuchoang.ecp.payment.internal.application.get.query.GetOrderPaymentService;
import org.phuchoang.ecp.payment.internal.application.order.command.RecordOrderCreatedService;
import org.phuchoang.ecp.payment.internal.application.selection.command.SelectCheckoutPaymentMethodService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PaymentFacadeAdapter implements PaymentFacade {
  private final SelectCheckoutPaymentMethodService selection;
  private final InitiatePaymentService initiation;
  private final PaymentApiMapper mapper;
  private final GetOrderPaymentService payments;
  private final ReceivePaymentProviderNotificationService callbacks;
  private final RecordOrderCreatedService orders;

  @Override
  public void selectCheckoutPaymentMethod(IdentityActor caller, UUID id, PaymentMethod method) {
    selection.select(caller, id, method.name());
  }

  @Override
  public Initiation initiatePayment(IdentityActor caller, UUID id, PaymentMethod method, String key, URI url,
      UUID correlation) {
    var result = initiation.initiate(new InitiatePaymentCommand(caller, id, method.name(), key, url, correlation));
    return mapper.toInitiation(result);
  }

  @Override
  public PaymentDetails getOrderPayment(UUID orderId) {
    return mapper.toPaymentDetails(payments.get(orderId));
  }

  @Override
  public CallbackResult receivePaymentProviderNotification(String ref, PaymentOutcome outcome, BigDecimal amount,
      String currency, String event, Instant at, Map<String, Object> payload, String signature, UUID correlation) {
    try {
      return CallbackResult.valueOf(callbacks
          .receive(new ProviderNotification(ref, outcome.name(), amount, currency, event, at, payload), signature,
              correlation)
          .name());
    } catch (ReceivePaymentProviderNotificationService.InvalidProviderSignature rejected) {
      throw new SecurityException("Provider signature could not be verified.");
    }
  }

  @Override
  public void recordOrderCreated(UUID id, UUID customer, BigDecimal total, String currency, String status, Instant at) {
    orders.record(id, customer, total, currency, status, at);
  }
}

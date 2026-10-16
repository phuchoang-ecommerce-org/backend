package org.phuchoang.ecp.messaging.payment;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.messaging.EventEnvelopeCodec;
import org.phuchoang.ecp.messaging.kafka.KafkaTopics;
import org.phuchoang.ecp.ordering.api.OrderFulfilmentFacade;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Payment outcomes cross back to Ordering only as durable events, preserving the acyclic module graph. */
@Component @RequiredArgsConstructor
class PaymentOutcomeListener {
    private final EventEnvelopeCodec codec;
    private final OrderFulfilmentFacade orders;
    @KafkaListener(topics = KafkaTopics.PAYMENT_PAYMENT, groupId = "ecp.ordering-payment-outcome")
    void receive(String body) {
        var event = codec.deserialize(body);
        if (!"PaymentCaptured".equals(event.eventType()) && !"PaymentFailed".equals(event.eventType())) return;
        UUID orderId = UUID.fromString(event.payload().get("orderId").asString());
        if ("PaymentCaptured".equals(event.eventType())) orders.recordPaymentSucceeded(orderId, event.correlationId());
        else orders.recordPaymentFailed(orderId, event.correlationId());
    }
}

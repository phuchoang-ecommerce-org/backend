package org.phuchoang.ecp.messaging.payment;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.messaging.EventEnvelopeCodec;
import org.phuchoang.ecp.messaging.kafka.KafkaConsumerDefinition;
import org.phuchoang.ecp.messaging.kafka.KafkaTopics;
import org.phuchoang.ecp.payment.api.PaymentFacade;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consumer-local translation of OrderCreated: no Payment → Ordering compile-time edge. */
@Component @RequiredArgsConstructor
class PaymentOrderCreatedListener {
    private static final KafkaConsumerDefinition CONSUMER = new KafkaConsumerDefinition("ecp.payment-order-created");
    private final EventEnvelopeCodec codec;
    private final PaymentFacade payments;
    @KafkaListener(topics = KafkaTopics.ORDERING_ORDER, groupId = "ecp.payment-order-created")
    void receive(String body) {
        var event = codec.deserialize(body);
        if (!"OrderCreated".equals(event.eventType())) return;
        var payload = event.payload();
        payments.recordOrderCreated(event.aggregateId(), UUID.fromString(payload.get("customerId").asString()),
            new BigDecimal(payload.get("totalAmount").asString()), payload.get("currency").asString(),
            payload.get("status").asString(), event.occurredAt());
    }
}

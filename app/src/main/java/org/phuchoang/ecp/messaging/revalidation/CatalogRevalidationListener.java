package org.phuchoang.ecp.messaging.revalidation;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.phuchoang.ecp.messaging.EventEnvelope;
import org.phuchoang.ecp.messaging.EventEnvelopeCodec;
import org.phuchoang.ecp.messaging.kafka.KafkaTopics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * The Kafka transport edge of the Catalog → web revalidation path: receive, decode the envelope,
 * delegate. No SQL, no cache keys, no HTTP, no cryptography here.
 */
@Component
class CatalogRevalidationListener {

    private final EventEnvelopeCodec codec;
    private final CatalogRevalidationHandler handler;

    CatalogRevalidationListener(EventEnvelopeCodec codec, CatalogRevalidationHandler handler) {
        this.codec = codec;
        this.handler = handler;
    }

    @KafkaListener(topics = { KafkaTopics.CATALOG_PRODUCT, KafkaTopics.CATALOG_CATEGORY },
        groupId = "${ecp.revalidation.consumer-group:ecp.web-revalidation}")
    void consume(ConsumerRecord<String, String> record) {
        EventEnvelope event = codec.deserialize(record.value());
        handler.handle(event, record.value());
    }
}

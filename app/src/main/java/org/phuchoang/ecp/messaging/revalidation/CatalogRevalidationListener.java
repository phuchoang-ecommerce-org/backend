package org.phuchoang.ecp.messaging.revalidation;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.phuchoang.ecp.messaging.EventEnvelope;
import org.phuchoang.ecp.messaging.EventEnvelopeCodec;
import org.phuchoang.ecp.messaging.kafka.KafkaTopics;
import org.phuchoang.ecp.web.common.filter.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * The Kafka transport edge of the Catalog → web revalidation path: receive, decode the envelope,
 * delegate. No SQL, no cache keys, no HTTP, no cryptography here.
 */
@Component
class CatalogRevalidationListener {

    private static final Logger log = LoggerFactory.getLogger(CatalogRevalidationListener.class);

    private final EventEnvelopeCodec codec;
    private final CatalogRevalidationHandler handler;

    CatalogRevalidationListener(EventEnvelopeCodec codec, CatalogRevalidationHandler handler) {
        this.codec = codec;
        this.handler = handler;
    }

    @KafkaListener(topics = { KafkaTopics.CATALOG_PRODUCT, KafkaTopics.CATALOG_CATEGORY },
        containerFactory = "webRevalidationKafkaListenerContainerFactory",
        groupId = "${ecp.revalidation.consumer-group:ecp.web-revalidation}")
    void consume(ConsumerRecord<String, String> record) {
        EventEnvelope event = codec.deserialize(record.value());
        String previousCorrelationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        MDC.put(CorrelationIdFilter.MDC_KEY, event.correlationId().toString());
        try {
            handler.handle(event, record.value());
            log.info("Catalog revalidation consumed eventId={} eventType={} aggregateId={} topic={} partition={} offset={} callbackOutcome=accepted",
                event.eventId(), event.eventType(), event.aggregateId(), record.topic(), record.partition(), record.offset());
        } finally {
            if (previousCorrelationId == null) {
                MDC.remove(CorrelationIdFilter.MDC_KEY);
            } else {
                MDC.put(CorrelationIdFilter.MDC_KEY, previousCorrelationId);
            }
        }
    }
}

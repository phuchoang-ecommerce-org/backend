package org.phuchoang.ecp.messaging.search;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.phuchoang.ecp.catalog.api.search.CatalogSearchProjection;
import org.phuchoang.ecp.catalog.api.search.CatalogSearchProjectionEvent;
import org.phuchoang.ecp.messaging.EventEnvelope;
import org.phuchoang.ecp.messaging.EventEnvelopeCodec;
import org.phuchoang.ecp.messaging.kafka.KafkaTopics;
import org.phuchoang.ecp.web.common.filter.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Kafka transport edge for Catalog's Elasticsearch projection; Catalog owns all index semantics. */
@Component
class CatalogSearchProjectionListener {

    private static final Logger log = LoggerFactory.getLogger(CatalogSearchProjectionListener.class);

    private final EventEnvelopeCodec codec;
    private final CatalogSearchProjection projection;

    CatalogSearchProjectionListener(EventEnvelopeCodec codec, CatalogSearchProjection projection) {
        this.codec = codec;
        this.projection = projection;
    }

    @KafkaListener(topics = { KafkaTopics.CATALOG_PRODUCT, KafkaTopics.CATALOG_CATEGORY },
        containerFactory = "catalogSearchKafkaListenerContainerFactory",
        groupId = "${ecp.search-projection.consumer-group:ecp.catalog-search}")
    void consume(ConsumerRecord<String, String> record) {
        EventEnvelope event = codec.deserialize(record.value());
        String previousCorrelationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        MDC.put(CorrelationIdFilter.MDC_KEY, event.correlationId().toString());
        try {
            projection.project(new CatalogSearchProjectionEvent(event.eventId(), event.eventType(), event.occurredAt(),
                event.aggregateType(), event.aggregateId(), event.correlationId(), event.payload()));
            log.info("Catalog search projection consumed eventId={} eventType={} aggregateId={} topic={} partition={} offset={}",
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

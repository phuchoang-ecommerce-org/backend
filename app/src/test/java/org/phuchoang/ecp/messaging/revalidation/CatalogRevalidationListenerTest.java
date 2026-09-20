package org.phuchoang.ecp.messaging.revalidation;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.messaging.EventEnvelope;
import org.phuchoang.ecp.messaging.EventEnvelopeCodec;
import org.phuchoang.ecp.messaging.OutboxRecords;
import org.phuchoang.ecp.messaging.kafka.KafkaTopics;
import org.phuchoang.ecp.web.common.filter.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doAnswer;

/** Kafka payload → decoded envelope → handler; nothing else. */
class CatalogRevalidationListenerTest {

    @Test
    void decodesTheEnvelopeAndDelegatesWithTheRawBody() {
        EventEnvelopeCodec codec = new EventEnvelopeCodec(JsonMapper.builder().build());
        CatalogRevalidationHandler handler = mock(CatalogRevalidationHandler.class);
        EventEnvelope expected = codec.fromOutbox(OutboxRecords.categoryEvent(null, null));
        String body = codec.serialize(expected);

        new CatalogRevalidationListener(codec, handler)
            .consume(new ConsumerRecord<>(KafkaTopics.CATALOG_CATEGORY, 0, 0L, expected.aggregateId().toString(), body));

        verify(handler).handle(expected, body);
    }

    @Test
    void subscribesToTheCatalogueTopics() throws Exception {
        KafkaListener listener = CatalogRevalidationListener.class
            .getDeclaredMethod("consume", ConsumerRecord.class).getAnnotation(KafkaListener.class);
        assertThat(listener.topics()).containsExactly(KafkaTopics.CATALOG_PRODUCT, KafkaTopics.CATALOG_CATEGORY);
    }

    @Test
    void restoresTheEnvelopeCorrelationIdForHandlingWithoutLeakingItToTheConsumerThread() {
        EventEnvelopeCodec codec = new EventEnvelopeCodec(JsonMapper.builder().build());
        CatalogRevalidationHandler handler = mock(CatalogRevalidationHandler.class);
        EventEnvelope event = codec.fromOutbox(OutboxRecords.categoryEvent(null, null));
        String body = codec.serialize(event);
        MDC.put(CorrelationIdFilter.MDC_KEY, "previous-correlation-id");
        doAnswer(invocation -> {
            assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isEqualTo(event.correlationId().toString());
            return null;
        }).when(handler).handle(event, body);

        new CatalogRevalidationListener(codec, handler)
            .consume(new ConsumerRecord<>(KafkaTopics.CATALOG_CATEGORY, 0, 0L, event.aggregateId().toString(), body));

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isEqualTo("previous-correlation-id");
        MDC.clear();
    }
}

package org.phuchoang.ecp.messaging.revalidation;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.messaging.EventEnvelope;
import org.phuchoang.ecp.messaging.EventEnvelopeCodec;
import org.phuchoang.ecp.messaging.OutboxRecords;
import org.phuchoang.ecp.messaging.kafka.KafkaTopics;
import org.springframework.kafka.annotation.KafkaListener;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
}

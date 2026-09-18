package org.phuchoang.ecp.messaging.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.messaging.EventEnvelope;
import org.phuchoang.ecp.messaging.EventEnvelopeCodec;
import org.phuchoang.ecp.messaging.OutboxRecords;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class KafkaEventPublisherTest {

    @Test
    @SuppressWarnings("unchecked")
    void derivesKafkaKeyAndMirrorHeadersFromTheEnvelope() {
        EventEnvelopeCodec codec = new EventEnvelopeCodec(JsonMapper.builder().build());
        EventEnvelope envelope = codec.fromOutbox(OutboxRecords.categoryEvent(null, null));
        KafkaEventPublisher publisher = new KafkaEventPublisher(mock(KafkaTemplate.class), codec);

        ProducerRecord<String, String> record = publisher.recordFor("ecp.catalog.category.v1", envelope);

        assertThat(record.topic()).isEqualTo("ecp.catalog.category.v1");
        assertThat(record.key()).isEqualTo(envelope.aggregateId().toString());
        assertThat(header(record, "ecp-event-id")).isEqualTo(envelope.eventId().toString());
        assertThat(header(record, "ecp-event-type")).isEqualTo("CategoryChanged");
        assertThat(header(record, "ecp-event-version")).isEqualTo("1");
        assertThat(header(record, "ecp-correlation-id")).isEqualTo(envelope.correlationId().toString());
        assertThat(header(record, "ecp-occurred-at")).isEqualTo("2026-09-15T00:00:00Z");
        assertThat(record.value()).isEqualTo(codec.serialize(envelope));
    }

    private static String header(ProducerRecord<String, String> record, String name) {
        return new String(record.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
    }
}

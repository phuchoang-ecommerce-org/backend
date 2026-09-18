package org.phuchoang.ecp.messaging.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.phuchoang.ecp.messaging.EventEnvelope;
import org.phuchoang.ecp.messaging.EventEnvelopeCodec;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * The sole Kafka producer adapter. {@link #publish} returns only after the broker has acknowledged
 * the record — the relay relies on that to mark the outbox row afterwards, never before.
 */
@Component
public class KafkaEventPublisher {

    private final KafkaTemplate<String, String> kafka;
    private final EventEnvelopeCodec codec;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafka, EventEnvelopeCodec codec) {
        this.kafka = kafka;
        this.codec = codec;
    }

    public void publish(String topic, EventEnvelope envelope) throws Exception {
        kafka.send(recordFor(topic, envelope)).get();
    }

    /** Key = aggregate id (per-aggregate ordering); mirror headers let consumers filter without parsing. */
    ProducerRecord<String, String> recordFor(String topic, EventEnvelope envelope) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, envelope.aggregateId().toString(),
            codec.serialize(envelope));
        record.headers().add(new RecordHeader("ecp-event-id", utf8(envelope.eventId().toString())));
        record.headers().add(new RecordHeader("ecp-event-type", utf8(envelope.eventType())));
        record.headers().add(new RecordHeader("ecp-event-version", utf8(Integer.toString(envelope.eventVersion()))));
        record.headers().add(new RecordHeader("ecp-correlation-id", utf8(envelope.correlationId().toString())));
        record.headers().add(new RecordHeader("ecp-occurred-at", utf8(envelope.occurredAt().toString())));
        return record;
    }

    private static byte[] utf8(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}

package org.phuchoang.ecp.events;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/** The sole Kafka producer adapter: body and headers are derived from one outbox record. */
@Component
class KafkaOutboxPublisher {

    private final KafkaTemplate<String, String> kafka;

    KafkaOutboxPublisher(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    void publish(OutboxRecord event) throws Exception {
        kafka.send(recordFor(event)).get();
    }

    static ProducerRecord<String, String> recordFor(OutboxRecord event) {
        ProducerRecord<String, String> record = new ProducerRecord<>(event.topic(), event.aggregateId().toString(),
            EventEnvelopeJson.serialize(event));
        record.headers().add(new RecordHeader("ecp-event-id", utf8(event.eventId().toString())));
        record.headers().add(new RecordHeader("ecp-event-type", utf8(event.eventType())));
        record.headers().add(new RecordHeader("ecp-event-version", utf8(Integer.toString(event.eventVersion()))));
        record.headers().add(new RecordHeader("ecp-correlation-id", utf8(event.correlationId().toString())));
        record.headers().add(new RecordHeader("ecp-occurred-at", utf8(event.occurredAt().toString())));
        return record;
    }

    private static byte[] utf8(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}

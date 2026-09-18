package org.phuchoang.ecp.messaging.outbox;

import org.phuchoang.ecp.messaging.EventEnvelopeCodec;
import org.phuchoang.ecp.messaging.kafka.KafkaEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * At-least-once relay from each module's outbox table to Kafka. The orchestration rule is the whole
 * class: for each module, under that module's lock, publish the oldest unpublished rows in
 * sequence order; broker acknowledgement always precedes the durable published mark, so a mark
 * that fails leaves the row pending and it is delivered again — intentional redelivery, not a bug.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxStore outbox;
    private final KafkaEventPublisher publisher;
    private final EventEnvelopeCodec codec;
    private final OutboxRelayLock lock;
    private final OutboxProperties properties;

    public OutboxRelay(OutboxStore outbox, KafkaEventPublisher publisher, EventEnvelopeCodec codec,
            OutboxRelayLock lock, OutboxProperties properties) {
        this.outbox = outbox;
        this.publisher = publisher;
        this.codec = codec;
        this.lock = lock;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${ecp.outbox.poll-delay-ms:1000}")
    void relay() {
        OutboxModule.all().forEach(this::relayModule);
    }

    public void relayModule(OutboxModule module) {
        try {
            lock.executeIfAcquired(module, () -> publishBatch(module));
        } catch (Exception exception) {
            log.warn("Outbox relay could not process module={}", module.nameValue(), exception);
        }
    }

    private void publishBatch(OutboxModule module) {
        List<OutboxRecord> records = outbox.unpublished(module, properties.batchSize());
        for (OutboxRecord record : records) {
            try {
                publisher.publish(record.topic(), codec.fromOutbox(record)); // waits for the broker ack
            } catch (Exception exception) {
                outbox.recordPublishFailure(module, record, exception);
                log.warn("Outbox publish failed module={} eventId={}", module.nameValue(), record.eventId(), exception);
                return; // preserve sequence order; retry this event before later rows
            }
            try {
                outbox.markPublished(module, record);
            } catch (Exception exception) {
                // The broker has the message but the row stays pending: intentional redelivery after recovery.
                log.warn("Broker acknowledged eventId={} but its outbox mark failed; it will be redelivered",
                    record.eventId(), exception);
                return;
            }
        }
    }
}

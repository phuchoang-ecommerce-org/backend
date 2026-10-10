package org.phuchoang.ecp.messaging.outbox;

import org.phuchoang.ecp.messaging.EventEnvelopeCodec;
import org.phuchoang.ecp.messaging.kafka.KafkaEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Deliberately republishes a bounded outbox sequence range. It never changes publication marks:
 * replay is an explicit at-least-once delivery and downstream idempotency remains authoritative.
 */
@Service
public class OutboxReplayService {
    private final OutboxStore outbox;
    private final KafkaEventPublisher publisher;
    private final EventEnvelopeCodec codec;

    public OutboxReplayService(OutboxStore outbox, KafkaEventPublisher publisher, EventEnvelopeCodec codec) {
        this.outbox = outbox;
        this.publisher = publisher;
        this.codec = codec;
    }

    public void replay(OutboxModule module, long firstSequence, long lastSequence) {
        for (OutboxRecord record : outbox.eventRange(module, firstSequence, lastSequence)) {
            try {
                publisher.publish(record.topic(), codec.fromOutbox(record));
            } catch (Exception exception) {
                throw new IllegalStateException("Outbox replay failed at event " + record.eventId(), exception);
            }
        }
    }
}

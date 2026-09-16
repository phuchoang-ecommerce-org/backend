package org.phuchoang.ecp.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

/**
 * At-least-once relay: broker acknowledgement always precedes the durable published mark. A
 * session advisory lock gives each module stream one active publisher across application replicas.
 */
@Component
class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private final DataSource dataSource;
    private final OutboxRepository outbox;
    private final KafkaOutboxPublisher publisher;
    private final int batchSize;

    OutboxRelay(DataSource dataSource, OutboxRepository outbox, KafkaOutboxPublisher publisher,
            @Value("${ecp.outbox.batch-size:100}") int batchSize) {
        this.dataSource = dataSource;
        this.outbox = outbox;
        this.publisher = publisher;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${ecp.outbox.poll-delay-ms:1000}")
    void relay() {
        OutboxModule.all().forEach(this::relayModule);
    }

    void relayModule(OutboxModule module) {
        try (Connection connection = dataSource.getConnection()) {
            if (!tryLock(connection, module)) {
                return;
            }
            try {
                publishBatch(module);
            } finally {
                unlock(connection, module);
            }
        } catch (Exception exception) {
            log.warn("Outbox relay could not process module={}", module.nameValue(), exception);
        }
    }

    private void publishBatch(OutboxModule module) {
        List<OutboxRecord> records = outbox.unpublished(module, batchSize);
        for (OutboxRecord event : records) {
            try {
                publisher.publish(event); // waits for the broker ack before touching published_at
            } catch (Exception exception) {
                outbox.recordPublishFailure(module, event, exception);
                log.warn("Outbox publish failed module={} eventId={}", module.nameValue(), event.eventId(), exception);
                return; // preserve sequence order; retry this event before later rows
            }
            try {
                outbox.markPublished(module, event);
            } catch (Exception exception) {
                // The broker has the message but the row stays pending: intentional redelivery after recovery.
                log.warn("Broker acknowledged eventId={} but its outbox mark failed; it will be redelivered", event.eventId(),
                    exception);
                return;
            }
        }
    }

    private static boolean tryLock(Connection connection, OutboxModule module) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_try_advisory_lock(hashtext(?))")) {
            statement.setString(1, "ecp.outbox." + module.nameValue());
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getBoolean(1);
            }
        }
    }

    private static void unlock(Connection connection, OutboxModule module) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_unlock(hashtext(?))")) {
            statement.setString(1, "ecp.outbox." + module.nameValue());
            statement.execute();
        }
    }
}

package org.phuchoang.ecp.messaging.outbox;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.messaging.EventEnvelopeCodec;
import org.phuchoang.ecp.messaging.kafka.KafkaEventPublisher;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The at-least-once relay rules, protected explicitly: ack before mark, stop on failure, redeliver on mark failure. */
class OutboxRelayTest {

    private final OutboxStore store = mock(OutboxStore.class);
    private final KafkaEventPublisher publisher = mock(KafkaEventPublisher.class);
    private final OutboxRelayLock lock = mock(OutboxRelayLock.class);
    private final OutboxRelay relay = new OutboxRelay(store, publisher,
        new EventEnvelopeCodec(JsonMapper.builder().build()), lock, new OutboxProperties(100, 1000));

    @Test
    void lockUnavailableSkipsTheModuleEntirely() {
        when(lock.executeIfAcquired(eq(OutboxModule.CATALOG), any())).thenReturn(false);

        relay.relayModule(OutboxModule.CATALOG);

        verify(store, never()).unpublished(any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void aSuccessfulPublishIsMarkedOnlyAfterTheBrokerAcknowledged() throws Exception {
        runUnderLock();
        OutboxRecord first = record(1);
        OutboxRecord second = record(2);
        when(store.unpublished(OutboxModule.CATALOG, 100)).thenReturn(List.of(first, second));

        relay.relayModule(OutboxModule.CATALOG);

        var order = inOrder(publisher, store);
        order.verify(publisher).publish(eq(first.topic()), envelopeOf(first));
        order.verify(store).markPublished(OutboxModule.CATALOG, first);
        order.verify(publisher).publish(eq(second.topic()), envelopeOf(second));
        order.verify(store).markPublished(OutboxModule.CATALOG, second);
    }

    @Test
    void aPublishFailureIsRecordedAndStopsTheOrderedBatch() throws Exception {
        runUnderLock();
        OutboxRecord first = record(1);
        OutboxRecord second = record(2);
        when(store.unpublished(OutboxModule.CATALOG, 100)).thenReturn(List.of(first, second));
        doThrow(new IllegalStateException("broker down")).when(publisher).publish(eq(first.topic()), envelopeOf(first));

        relay.relayModule(OutboxModule.CATALOG);

        verify(store).recordPublishFailure(eq(OutboxModule.CATALOG), eq(first), any(IllegalStateException.class));
        verify(store, never()).markPublished(any(), any());
        verify(publisher, never()).publish(eq(second.topic()), envelopeOf(second));
    }

    @Test
    void aMarkFailureAfterAcknowledgementLeavesTheRowPendingForRedelivery() throws Exception {
        runUnderLock();
        OutboxRecord first = record(1);
        OutboxRecord second = record(2);
        when(store.unpublished(OutboxModule.CATALOG, 100)).thenReturn(List.of(first, second));
        doThrow(new IllegalStateException("db down")).when(store).markPublished(OutboxModule.CATALOG, first);

        relay.relayModule(OutboxModule.CATALOG); // must not throw — the row simply stays unpublished

        verify(publisher).publish(eq(first.topic()), envelopeOf(first));
        verify(store, never()).recordPublishFailure(any(), any(), any());
        verify(publisher, never()).publish(eq(second.topic()), envelopeOf(second));
    }

    private void runUnderLock() {
        doAnswer(invocation -> {
            invocation.getArgument(1, Runnable.class).run();
            return true;
        }).when(lock).executeIfAcquired(eq(OutboxModule.CATALOG), any());
    }

    private static org.phuchoang.ecp.messaging.EventEnvelope envelopeOf(OutboxRecord record) {
        return org.mockito.ArgumentMatchers.argThat(envelope -> envelope != null && envelope.eventId().equals(record.eventId()));
    }

    private static OutboxRecord record(long sequence) {
        return new OutboxRecord(sequence, UUID.randomUUID(), "ProductUpdated", 1, Instant.now(), "Product",
            UUID.randomUUID(), UUID.randomUUID(), null, null, "{\"productId\":\"x\"}", "ecp.catalog.product.v1");
    }
}

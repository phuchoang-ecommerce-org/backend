package org.phuchoang.ecp.messaging.revalidation;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Consumer failure classification: permanent trust failures skip blocking retries. */
class WebRevalidationKafkaConfigurationTest {

    @Test
    @SuppressWarnings("unchecked")
    void signatureRejectionIsPublishedImmediatelyToTheConsumerScopedDlt() {
        KafkaTemplate<Object, Object> kafka = mock(KafkaTemplate.class);
        String dltTopic = "ecp.catalog.product.v1.dlt.web-revalidation";
        Consumer<String, String> consumer = mock(Consumer.class);
        when(consumer.partitionsFor(eq(dltTopic), any(Duration.class))).thenReturn(List.of(
            new PartitionInfo(dltTopic, 2, Node.noNode(), new Node[] { Node.noNode() }, new Node[] { Node.noNode() })));
        when(kafka.send(any(ProducerRecord.class))).thenAnswer(invocation -> {
            ProducerRecord<?, ?> dltRecord = invocation.getArgument(0);
            assertThat(dltRecord.topic()).isEqualTo(dltTopic);
            assertThat(dltRecord.partition()).isEqualTo(2);
            return CompletableFuture.completedFuture(null);
        });
        DefaultErrorHandler handler = WebRevalidationKafkaConfiguration.errorHandler(kafka);
        ConsumerRecord<String, String> source = new ConsumerRecord<>("ecp.catalog.product.v1", 2, 18L, "key", "body");

        boolean recovered = handler.handleOne(new RevalidationSignatureRejectedException(UUID.randomUUID()), source,
            consumer, mock());

        assertThat(recovered).isTrue();
        verify(kafka).send(any(ProducerRecord.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void anUnclassifiedFailureRemainsOnTheRetryPathBeforeDltRecovery() {
        KafkaTemplate<Object, Object> kafka = mock(KafkaTemplate.class);
        DefaultErrorHandler handler = WebRevalidationKafkaConfiguration.errorHandler(kafka);
        ConsumerRecord<String, String> source = new ConsumerRecord<>("ecp.catalog.product.v1", 2, 18L, "key", "body");

        boolean recovered = handler.handleOne(new IllegalStateException("HTTP 500"), source, mock(), mock());

        assertThat(recovered).isFalse();
        verify(kafka, never()).send(any(ProducerRecord.class));
    }
}

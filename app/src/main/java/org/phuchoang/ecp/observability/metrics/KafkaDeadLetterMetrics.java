package org.phuchoang.ecp.observability.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.TopicPartition;
import org.phuchoang.ecp.messaging.kafka.KafkaTopicCatalogue;
import org.phuchoang.ecp.messaging.kafka.KafkaTopicDefinition;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/** Approximate retained-record depth for every consumer-specific DLT, exposed as a live gauge. */
@Component
class KafkaDeadLetterMetrics {
    private static final Duration ADMIN_TIMEOUT = Duration.ofSeconds(2);
    private final KafkaAdmin kafkaAdmin;

    KafkaDeadLetterMetrics(MeterRegistry registry, KafkaAdmin kafkaAdmin, KafkaTopicCatalogue catalogue) {
        this.kafkaAdmin = kafkaAdmin;
        for (KafkaTopicDefinition topic : catalogue.allTopics()) {
            if (topic.consumers().isEmpty() && topic.name().contains(".dlt.")) {
                Gauge.builder("ecp.kafka.dead_letter.depth", this, ignored -> depth(topic))
                    .tag("topic", topic.name()).description("Retained records in a consumer dead-letter topic").register(registry);
            }
        }
    }

    private double depth(KafkaTopicDefinition topic) {
        try (AdminClient admin = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            var descriptions = admin.describeTopics(java.util.List.of(topic.name())).allTopicNames().get(ADMIN_TIMEOUT.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            Map<TopicPartition, org.apache.kafka.clients.admin.OffsetSpec> offsets = new java.util.HashMap<>();
            descriptions.get(topic.name()).partitions().forEach(partition ->
                offsets.put(new TopicPartition(topic.name(), partition.partition()), org.apache.kafka.clients.admin.OffsetSpec.latest()));
            return admin.listOffsets(offsets).all().get(ADMIN_TIMEOUT.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS).values().stream()
                .mapToLong(result -> result.offset()).sum();
        } catch (Exception ignored) {
            return Double.NaN; // a failed observability lookup must not interfere with consumption
        }
    }
}

package org.phuchoang.ecp.messaging.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.TopicDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Warns about a manually altered broker without turning observability into a readiness dependency. */
@Component
class KafkaTopicDivergenceLogger {

    private static final Logger log = LoggerFactory.getLogger(KafkaTopicDivergenceLogger.class);

    private final KafkaAdmin kafkaAdmin;
    private final KafkaTopicCatalogue catalogue;

    KafkaTopicDivergenceLogger(KafkaAdmin kafkaAdmin, KafkaTopicCatalogue catalogue) {
        this.kafkaAdmin = kafkaAdmin;
        this.catalogue = catalogue;
    }

    @EventListener(ApplicationReadyEvent.class)
    void warnOnDivergence() {
        try (AdminClient admin = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            Map<String, TopicDescription> actual = admin.describeTopics(catalogue.allTopics().stream()
                .map(KafkaTopicDefinition::name).toList()).allTopicNames().get(5, TimeUnit.SECONDS);
            for (KafkaTopicDefinition expected : catalogue.allTopics()) {
                TopicDescription topic = actual.get(expected.name());
                if (topic == null || topic.partitions().size() != expected.partitions()) {
                    log.warn("Kafka topic divergence topic={} expectedPartitions={} actualPartitions={}", expected.name(),
                        expected.partitions(), topic == null ? "missing" : topic.partitions().size());
                }
            }
        } catch (Exception exception) {
            log.warn("Kafka topic divergence check unavailable; readiness remains unaffected", exception);
        }
    }
}

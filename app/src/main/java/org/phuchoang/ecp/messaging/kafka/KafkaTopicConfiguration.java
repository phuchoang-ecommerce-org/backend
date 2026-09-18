package org.phuchoang.ecp.messaging.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.Map;

/** Creates the declared topics at application startup; the broker itself never auto-creates them. */
@Configuration
class KafkaTopicConfiguration {

    @Bean
    KafkaAdmin.NewTopics eventTopics(KafkaTopicCatalogue catalogue) {
        NewTopic[] topics = catalogue.allTopics().stream()
            .map(topic -> new NewTopic(topic.name(), topic.partitions(), (short) 1).configs(Map.of(
                "retention.ms", Long.toString(topic.retention().toMillis()),
                "cleanup.policy", "delete",
                "min.insync.replicas", "1")))
            .toArray(NewTopic[]::new);
        return new KafkaAdmin.NewTopics(topics);
    }
}

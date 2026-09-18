package org.phuchoang.ecp.messaging.kafka;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The app-owned physical catalogue: every source topic, its partitions, retention and consumers,
 * plus one dead-letter topic per (source, consumer) pair. Future publishers extend this rather
 * than inventing topics.
 */
@Component
public class KafkaTopicCatalogue {

    static final Duration EVENT_RETENTION = Duration.ofDays(30);
    static final Duration DLT_RETENTION = Duration.ofDays(90);

    public List<KafkaTopicDefinition> allTopics() {
        List<KafkaTopicDefinition> topics = new ArrayList<>();
        for (KafkaTopicDefinition source : sources()) {
            topics.add(source);
            for (KafkaConsumerDefinition consumer : source.consumers()) {
                topics.add(source.deadLetterTopicFor(consumer, DLT_RETENTION));
            }
        }
        return List.copyOf(topics);
    }

    public List<KafkaTopicDefinition> sources() {
        return List.of(
            source(KafkaTopics.ORDERING_ORDER, 12, "ecp.payment", "ecp.shipping", "ecp.review", "ecp.catalog",
                "ecp.notification", "ecp.audit", "ecp.reporting"),
            source(KafkaTopics.PAYMENT_PAYMENT, 6, "ecp.ordering", "ecp.notification", "ecp.audit", "ecp.reporting"),
            source(KafkaTopics.INVENTORY_STOCK_ITEM, 6, "ecp.catalog", "ecp.audit", "ecp.reporting"),
            source(KafkaTopics.CATALOG_PRODUCT, 6, "ecp.catalog", "ecp.web-revalidation", "ecp.audit", "ecp.reporting"),
            source(KafkaTopics.CATALOG_CATEGORY, 3, "ecp.catalog", "ecp.web-revalidation", "ecp.audit", "ecp.reporting"),
            source(KafkaTopics.SHIPPING_SHIPMENT, 3, "ecp.ordering", "ecp.notification", "ecp.reporting"),
            source(KafkaTopics.PROMOTION_PROMOTION, 3, "ecp.audit", "ecp.reporting"),
            source(KafkaTopics.REVIEW_REVIEW, 3, "ecp.catalog", "ecp.notification", "ecp.audit"),
            source(KafkaTopics.IDENTITY_ACCOUNT, 3, "ecp.audit"));
    }

    private static KafkaTopicDefinition source(String name, int partitions, String... consumers) {
        return new KafkaTopicDefinition(name, partitions, EVENT_RETENTION,
            Arrays.stream(consumers).map(KafkaConsumerDefinition::new).toList());
    }
}

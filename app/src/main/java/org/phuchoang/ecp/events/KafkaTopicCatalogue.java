package org.phuchoang.ecp.events;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** The app-owned physical catalogue; future publishers extend this rather than inventing topics. */
@Component
class KafkaTopicCatalogue {

    static final long EVENT_RETENTION_MS = 2_592_000_000L; // 30 days
    static final long DLT_RETENTION_MS = 7_776_000_000L; // 90 days

    List<TopicDefinition> allTopics() {
        List<TopicDefinition> topics = new ArrayList<>();
        for (TopicDefinition source : sources()) {
            topics.add(source);
            for (String consumer : source.consumers()) {
                topics.add(new TopicDefinition(source.name() + ".dlt." + consumer.substring("ecp.".length()),
                    source.partitions(), DLT_RETENTION_MS, List.of()));
            }
        }
        return List.copyOf(topics);
    }

    List<TopicDefinition> sources() {
        return List.of(
            source("ecp.ordering.order.v1", 12, "ecp.payment", "ecp.shipping", "ecp.review", "ecp.catalog",
                "ecp.notification", "ecp.audit", "ecp.reporting"),
            source("ecp.payment.payment.v1", 6, "ecp.ordering", "ecp.notification", "ecp.audit", "ecp.reporting"),
            source("ecp.inventory.stockitem.v1", 6, "ecp.catalog", "ecp.audit", "ecp.reporting"),
            source("ecp.catalog.product.v1", 6, "ecp.catalog", "ecp.audit", "ecp.reporting"),
            source("ecp.catalog.category.v1", 3, "ecp.catalog", "ecp.audit", "ecp.reporting"),
            source("ecp.shipping.shipment.v1", 3, "ecp.ordering", "ecp.notification", "ecp.reporting"),
            source("ecp.promotion.promotion.v1", 3, "ecp.audit", "ecp.reporting"),
            source("ecp.review.review.v1", 3, "ecp.catalog", "ecp.notification", "ecp.audit"),
            source("ecp.identity.account.v1", 3, "ecp.audit"));
    }

    private static TopicDefinition source(String name, int partitions, String... consumers) {
        return new TopicDefinition(name, partitions, EVENT_RETENTION_MS, List.of(consumers));
    }

    record TopicDefinition(String name, int partitions, long retentionMs, List<String> consumers) {
    }
}

package org.phuchoang.ecp.messaging.kafka;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaTopicCatalogueTest {

    private final KafkaTopicCatalogue catalogue = new KafkaTopicCatalogue();

    @Test
    void catalogueIncludesAConsumerScopedDltForEveryDeclaredSubscription() {
        Set<String> names = catalogue.allTopics().stream().map(KafkaTopicDefinition::name).collect(Collectors.toSet());

        assertThat(names).contains(KafkaTopics.CATALOG_CATEGORY, "ecp.catalog.category.v1.dlt.catalog",
            "ecp.catalog.category.v1.dlt.web-revalidation", "ecp.catalog.category.v1.dlt.audit",
            "ecp.catalog.category.v1.dlt.reporting");
        assertThat(catalogue.allTopics().stream().filter(topic -> topic.name().contains(".dlt."))
            .allMatch(topic -> topic.retention().equals(Duration.ofDays(90)))).isTrue();
        assertThat(catalogue.sources()).allMatch(topic -> topic.retention().equals(Duration.ofDays(30)));
    }

    @Test
    void theListenerSubscribesToTopicsTheCatalogueProvisions() {
        Set<String> sources = catalogue.sources().stream().map(KafkaTopicDefinition::name).collect(Collectors.toSet());
        assertThat(sources).contains(KafkaTopics.CATALOG_PRODUCT, KafkaTopics.CATALOG_CATEGORY);
    }
}

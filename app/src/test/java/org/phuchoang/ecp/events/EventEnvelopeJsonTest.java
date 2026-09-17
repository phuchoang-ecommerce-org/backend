package org.phuchoang.ecp.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventEnvelopeJsonTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CATEGORY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CORRELATION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    void serializesThePersistedCategoryEventAsTheVersionedContract() throws Exception {
        OutboxRecord event = categoryEvent();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode envelope = mapper.readTree(EventEnvelopeJson.serialize(event));
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder ->
            builder.schemaLoaders(loaders -> loaders.schemas(uri -> {
                if (uri.endsWith("/envelope.v1.json")) {
                    return resource("/event-contracts/envelope.v1.json");
                }
                return null;
            })));
        try (InputStream schemaFile = getClass().getResourceAsStream("/event-contracts/catalog/CategoryChanged.v1.json")) {
            JsonSchema schema = factory.getSchema(URI.create(
                "https://ecp.phuchoang.org/event-contracts/catalog/CategoryChanged.v1.json"), mapper.readTree(schemaFile));
            assertThat(schema.validate(envelope)).isEmpty();
        }
        assertThat(envelope.at("/payload/id").asText()).isEqualTo(CATEGORY_ID.toString());
    }

    @Test
    void derivesKafkaKeyAndMirrorHeadersFromTheEnvelope() {
        ProducerRecord<String, String> record = KafkaOutboxPublisher.recordFor(categoryEvent());
        assertThat(record.topic()).isEqualTo("ecp.catalog.category.v1");
        assertThat(record.key()).isEqualTo(CATEGORY_ID.toString());
        assertThat(new String(record.headers().lastHeader("ecp-event-id").value())).isEqualTo(EVENT_ID.toString());
        assertThat(new String(record.headers().lastHeader("ecp-correlation-id").value())).isEqualTo(CORRELATION_ID.toString());
    }

    @Test
    void catalogueIncludesAConsumerScopedDltForEveryDeclaredSubscription() {
        KafkaTopicCatalogue catalogue = new KafkaTopicCatalogue();
        Set<String> names = catalogue.allTopics().stream().map(KafkaTopicCatalogue.TopicDefinition::name)
            .collect(java.util.stream.Collectors.toSet());
        assertThat(names).contains("ecp.catalog.category.v1", "ecp.catalog.category.v1.dlt.catalog",
            "ecp.catalog.category.v1.dlt.audit", "ecp.catalog.category.v1.dlt.reporting");
        assertThat(catalogue.allTopics().stream().filter(topic -> topic.name().endsWith(".dlt.catalog"))
            .allMatch(topic -> topic.retentionMs() == KafkaTopicCatalogue.DLT_RETENTION_MS)).isTrue();
    }

    private static OutboxRecord categoryEvent() {
        return new OutboxRecord(1L, EVENT_ID, "CategoryChanged", 1, Instant.parse("2026-09-15T00:00:00Z"),
            "Category", CATEGORY_ID, CORRELATION_ID, null, null,
            "{\"id\":\"" + CATEGORY_ID + "\",\"parentId\":null,\"name\":\"Shoes\",\"slug\":\"shoes\","
                + "\"path\":\"/" + CATEGORY_ID + "/\",\"depth\":0,\"sortOrder\":0,\"affectedCategorySlugs\":[\"shoes\"]}",
            "ecp.catalog.category.v1");
    }

    private static String resource(String path) {
        try (InputStream stream = EventEnvelopeJsonTest.class.getResourceAsStream(path)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read test schema " + path, exception);
        }
    }
}

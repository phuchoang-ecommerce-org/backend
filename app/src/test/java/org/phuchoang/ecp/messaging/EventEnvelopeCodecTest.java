package org.phuchoang.ecp.messaging;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The typed envelope, serialised by Jackson, is the versioned wire contract
 * (`event-contracts/envelope.v1.json`) — and the same codec reads it back on the consumer side.
 */
class EventEnvelopeCodecTest {

    private final EventEnvelopeCodec codec = new EventEnvelopeCodec(JsonMapper.builder().build());

    @Test
    void serializesThePersistedCategoryEventAsTheVersionedContract() throws Exception {
        String body = codec.serialize(codec.fromOutbox(OutboxRecords.categoryEvent(null, null)));

        com.fasterxml.jackson.databind.ObjectMapper schemaMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode envelope = schemaMapper.readTree(body);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder ->
            builder.schemaLoaders(loaders -> loaders.schemas(uri -> {
                if (uri.endsWith("/envelope.v1.json")) {
                    return resource("/event-contracts/envelope.v1.json");
                }
                return null;
            })));
        try (InputStream schemaFile = getClass().getResourceAsStream("/event-contracts/catalog/CategoryChanged.v1.json")) {
            JsonSchema schema = factory.getSchema(URI.create(
                "https://ecp.phuchoang.org/event-contracts/catalog/CategoryChanged.v1.json"), schemaMapper.readTree(schemaFile));
            assertThat(schema.validate(envelope)).isEmpty();
        }
        assertThat(envelope.at("/payload/id").asText()).isEqualTo(OutboxRecords.CATEGORY_ID.toString());
        assertThat(envelope.has("actor")).as("actor is always written, as null for automated work").isTrue();
        assertThat(envelope.get("actor").isNull()).isTrue();
        assertThat(envelope.get("occurredAt").asText()).isEqualTo("2026-09-15T00:00:00Z");
    }

    @Test
    void writesTheActorWhenPresentAndRoundTripsThroughTheConsumerSide() {
        UUID userId = UUID.randomUUID();
        EventEnvelope produced = codec.fromOutbox(OutboxRecords.categoryEvent(userId, "STAFF"));

        EventEnvelope consumed = codec.deserialize(codec.serialize(produced));

        assertThat(consumed).isEqualTo(produced);
        assertThat(consumed.actor()).isEqualTo(new EventEnvelope.Actor(userId, "STAFF"));
        assertThat(consumed.payload().path("slug").asText()).isEqualTo("shoes");
        assertThat(consumed.occurredAt()).isEqualTo(Instant.parse("2026-09-15T00:00:00Z"));
    }

    private static String resource(String path) {
        try (InputStream stream = EventEnvelopeCodecTest.class.getResourceAsStream(path)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read test schema " + path, exception);
        }
    }
}

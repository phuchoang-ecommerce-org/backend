package org.phuchoang.ecp.catalog.internal.infrastructure.persistence.write.product;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/** MapStruct conversion methods for JSON columns owned by the catalog aggregate. */
@Component
class CatalogJsonMapper {
    private static final TypeReference<Map<String, Object>> OBJECTS = new TypeReference<>() { };
    private static final TypeReference<Map<String, String>> STRINGS = new TypeReference<>() { };

    private final ObjectMapper json;

    CatalogJsonMapper(ObjectMapper json) { this.json = json; }

    @Named("objectsToJson")
    String objectsToJson(Map<String, Object> value) { return write(value); }
    @Named("stringsToJson")
    String stringsToJson(Map<String, String> value) { return write(value); }
    @Named("jsonToObjects")
    Map<String, Object> jsonToObjects(String value) { return read(value, OBJECTS); }
    @Named("jsonToStrings")
    Map<String, String> jsonToStrings(String value) { return read(value, STRINGS); }

    private String write(Object value) {
        try { return json.writeValueAsString(value == null ? Map.of() : value); }
        catch (Exception exception) { throw new IllegalArgumentException("Invalid catalog JSON", exception); }
    }

    private <T> T read(String value, TypeReference<T> type) {
        try { return json.readValue(value, type); }
        catch (Exception exception) { throw new IllegalStateException("Invalid stored catalog JSON", exception); }
    }
}

package org.phuchoang.ecp.catalog.internal.application.search;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/** Internal projection input; transport-facing event contracts stop at Catalog's API adapter. */
public record SearchProjectionEvent(UUID eventId, String eventType, Instant occurredAt, String aggregateType,
        UUID aggregateId, UUID correlationId, JsonNode payload) {
}

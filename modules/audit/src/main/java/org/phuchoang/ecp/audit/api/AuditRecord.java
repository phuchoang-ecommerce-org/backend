package org.phuchoang.ecp.audit.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Immutable evidence for a significant business or security action. */
public record AuditRecord(UUID eventId, UUID actorId, String actorRole, String action, String entityType,
        UUID entityId, String module, Map<String, Object> beforeValue, Map<String, Object> afterValue,
        String reason, UUID correlationId, Instant occurredAt, String originatingProcess, boolean actorUnidentified) {

    public AuditRecord {
        if (eventId == null || action == null || action.isBlank() || entityType == null || entityType.isBlank()
                || module == null || module.isBlank() || occurredAt == null) {
            throw new IllegalArgumentException("Audit evidence requires identity, action, owner and time.");
        }
        beforeValue = beforeValue == null ? Map.of() : Map.copyOf(beforeValue);
        afterValue = afterValue == null ? Map.of() : Map.copyOf(afterValue);
        if (actorUnidentified && (originatingProcess == null || originatingProcess.isBlank())) {
            throw new IllegalArgumentException("An unidentified actor must be attributed to its originating process.");
        }
    }
}

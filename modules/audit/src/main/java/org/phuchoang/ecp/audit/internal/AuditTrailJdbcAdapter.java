package org.phuchoang.ecp.audit.internal;

import org.phuchoang.ecp.audit.api.AuditRecord;
import org.phuchoang.ecp.audit.api.AuditTrail;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** PostgreSQL append-only adapter; the database grants prohibit update and delete independently of this code. */
@Component
class AuditTrailJdbcAdapter implements AuditTrail {
    private final JdbcClient jdbc;
    private final ObjectMapper json;

    AuditTrailJdbcAdapter(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public void record(AuditRecord record) {
        jdbc.sql("""
                insert into audit_entry (id, event_id, actor_id, actor_role, action, entity_type, entity_id, module,
                    before_value, after_value, reason, correlation_id, occurred_at)
                values (:id, :eventId, :actorId, :actorRole, :action, :entityType, :entityId, :module,
                    cast(:beforeValue as jsonb), cast(:afterValue as jsonb), :reason, :correlationId, :occurredAt)
                """)
            .param("id", record.eventId()).param("eventId", record.eventId()).param("actorId", record.actorId())
            .param("actorRole", attributedRole(record)).param("action", record.action()).param("entityType", record.entityType())
            .param("entityId", record.entityId()).param("module", record.module())
            .param("beforeValue", serialize(record.beforeValue())).param("afterValue", serialize(record.afterValue()))
            .param("reason", record.reason()).param("correlationId", record.correlationId())
            .param("occurredAt", record.occurredAt()).update();
    }

    private String serialize(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Audit evidence could not be serialized.", exception);
        }
    }

    private static String attributedRole(AuditRecord record) {
        return record.actorUnidentified() ? "PROCESS:" + record.originatingProcess() + ":UNIDENTIFIED"
            : record.actorRole();
    }
}

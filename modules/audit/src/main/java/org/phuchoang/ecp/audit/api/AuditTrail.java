package org.phuchoang.ecp.audit.api;

/** Public synchronous audit boundary. Failure to record is deliberately allowed to abort the caller's transaction. */
public interface AuditTrail {
    void record(AuditRecord record);
}

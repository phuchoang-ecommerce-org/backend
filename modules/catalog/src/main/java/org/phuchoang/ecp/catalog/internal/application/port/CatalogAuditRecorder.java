package org.phuchoang.ecp.catalog.internal.application.port;

import org.phuchoang.ecp.sharedkernel.api.event.EventActor;

import java.util.UUID;

/** Replaceable transactional audit bridge; Sprint 12 replaces its stub adapter. */
public interface CatalogAuditRecorder {
    void record(UUID correlationId, EventActor actor, String action, String entityType, UUID entityId,
                String beforeValue, String afterValue, String reason);
}

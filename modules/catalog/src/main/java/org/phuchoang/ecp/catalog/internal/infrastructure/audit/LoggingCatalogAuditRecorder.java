package org.phuchoang.ecp.catalog.internal.infrastructure.audit;

import org.phuchoang.ecp.catalog.internal.application.port.CatalogAuditRecorder;
import org.phuchoang.ecp.sharedkernel.api.event.EventActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Sprint-09 fail-fast seam for the future append-only audit adapter. */
@Component
class LoggingCatalogAuditRecorder implements CatalogAuditRecorder {
    private static final Logger log = LoggerFactory.getLogger(LoggingCatalogAuditRecorder.class);
    @Override
    public void record(UUID correlationId, EventActor actor, String action, String entityType, UUID entityId,
            String beforeValue, String afterValue, String reason) {
        log.info("[stub-audit] action={} entityType={} entityId={} actor={} correlationId={} reason={}", action,
            entityType, entityId, actor == null ? null : actor.userId(), correlationId, reason);
    }
}

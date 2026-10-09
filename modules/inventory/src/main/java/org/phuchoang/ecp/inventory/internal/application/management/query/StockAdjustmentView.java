package org.phuchoang.ecp.inventory.internal.application.management.query;

import java.time.Instant;
import java.util.UUID;

/** Operational read model for an immutable stock adjustment. */
public record StockAdjustmentView(UUID id, UUID stockItemId, int delta, String reasonCode, String reason,
        UUID actorId, Instant occurredAt) { }

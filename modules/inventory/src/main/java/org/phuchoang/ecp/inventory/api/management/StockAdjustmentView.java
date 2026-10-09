package org.phuchoang.ecp.inventory.api.management;

import java.time.Instant;
import java.util.UUID;

public record StockAdjustmentView(UUID id, UUID stockItemId, int delta, String reasonCode, String reason,
        UUID actorId, Instant occurredAt) { }

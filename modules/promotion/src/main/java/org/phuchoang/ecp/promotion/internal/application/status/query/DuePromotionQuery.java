package org.phuchoang.ecp.promotion.internal.application.status.query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Finds promotion IDs whose lifecycle commands are ready for a scheduled sweep.
 */
public interface DuePromotionQuery {
    List<UUID> scheduledStatusChanges(Instant now, int limit);

    List<UUID> activations(Instant now, int limit);

    List<UUID> expirations(Instant now, int limit);
}

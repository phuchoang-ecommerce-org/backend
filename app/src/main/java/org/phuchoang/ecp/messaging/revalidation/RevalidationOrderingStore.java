package org.phuchoang.ecp.messaging.revalidation;

import java.time.Instant;
import java.util.UUID;

/**
 * Out-of-order protection per aggregate: Kafka orders within a partition only, and a redelivered
 * older event must never overwrite the effect of a newer one already applied.
 */
public interface RevalidationOrderingStore {

    /** @return {@code true} if {@code occurredAt} is newer than the aggregate's cursor (which is then advanced) */
    boolean advanceIfNewer(UUID aggregateId, Instant occurredAt);
}

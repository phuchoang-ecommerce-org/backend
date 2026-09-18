package org.phuchoang.ecp.messaging.outbox;

import java.util.List;

/**
 * Infrastructure persistence for the relay — deliberately not a DDD repository: the outbox is a
 * transport buffer, not an aggregate. Every operation is scoped to one {@link OutboxModule} table.
 */
public interface OutboxStore {

    /** The oldest unpublished rows in sequence order, at most {@code limit}. */
    List<OutboxRecord> unpublished(OutboxModule module, int limit);

    /** Records the broker's acknowledgement; a no-op if the row was marked concurrently. */
    void markPublished(OutboxModule module, OutboxRecord record);

    /** Bumps the attempt counter and stores the (truncated) failure for operators to read. */
    void recordPublishFailure(OutboxModule module, OutboxRecord record, Exception failure);

    double unpublishedDepth(OutboxModule module);

    double oldestUnpublishedLagSeconds(OutboxModule module);
}

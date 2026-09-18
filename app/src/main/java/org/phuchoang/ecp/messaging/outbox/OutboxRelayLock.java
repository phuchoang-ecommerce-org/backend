package org.phuchoang.ecp.messaging.outbox;

/** Gives each module's outbox stream exactly one active publisher across application replicas. */
public interface OutboxRelayLock {

    /**
     * Runs {@code operation} while holding the module's lock, or skips it when another replica holds
     * the lock right now.
     *
     * @return {@code true} if the lock was acquired and the operation ran
     */
    boolean executeIfAcquired(OutboxModule module, Runnable operation);
}

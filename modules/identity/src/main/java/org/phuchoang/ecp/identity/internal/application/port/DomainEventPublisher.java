package org.phuchoang.ecp.identity.internal.application.port;

import org.phuchoang.ecp.identity.internal.domain.model.Account;

/**
 * The single event-publication mechanism for identity workflows, so no use case can persist an
 * aggregate and forget to dispatch its events. Transport is the in-process Modulith bus
 * (`ADR-0012` §4) — identity has no outbox; the adapter decides that, not the use cases.
 */
public interface DomainEventPublisher {

    /** Publishes an application-raised event (one not buffered by an aggregate). */
    void publish(Object event);

    /** Drains and publishes every event {@code account} buffered since it was loaded. */
    void publishFrom(Account account);
}

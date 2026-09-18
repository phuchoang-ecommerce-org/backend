package org.phuchoang.ecp.identity.internal.infrastructure.event;

import org.phuchoang.ecp.identity.internal.application.port.DomainEventPublisher;
import org.phuchoang.ecp.identity.internal.domain.model.Account;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * {@link DomainEventPublisher} over Spring's in-process event bus, which Spring Modulith turns
 * into the `ADR-0012` §4 transport: listeners annotated {@code @ApplicationModuleListener} run
 * after the publishing transaction commits.
 */
@Component
class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher events;

    SpringDomainEventPublisher(ApplicationEventPublisher events) {
        this.events = events;
    }

    @Override
    public void publish(Object event) {
        events.publishEvent(event);
    }

    @Override
    public void publishFrom(Account account) {
        account.pullDomainEvents().forEach(events::publishEvent);
    }
}

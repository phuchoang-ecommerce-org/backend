package org.phuchoang.ecp.events;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/** Live database-backed outbox backlog and lag gauges, one tagged series per publisher module. */
@Component
class OutboxMetrics {

    OutboxMetrics(MeterRegistry registry, OutboxRepository outbox) {
        for (OutboxModule module : OutboxModule.all()) {
            Gauge.builder("ecp.outbox.unpublished.depth", outbox, ignored -> outbox.unpublishedDepth(module))
                .tag("module", module.nameValue()).description("Unpublished outbox rows").register(registry);
            Gauge.builder("ecp.outbox.oldest.unpublished.lag.seconds", outbox,
                    ignored -> outbox.oldestUnpublishedLagSeconds(module))
                .tag("module", module.nameValue()).description("Age of the oldest unpublished outbox row")
                .register(registry);
        }
    }
}

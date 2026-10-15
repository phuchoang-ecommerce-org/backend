package org.phuchoang.ecp.ordering.internal.application.place.command;

import org.phuchoang.ecp.identity.api.authorization.IdentityActor;

import java.time.Instant;
import java.util.UUID;

/** Ordering's durable event-write port; Kafka delivery is performed only by the shared relay. */
public interface OrderOutbox {
    void appendOrderCreated(UUID orderId, UUID correlationId, IdentityActor actor, Instant occurredAt, String payload);
}

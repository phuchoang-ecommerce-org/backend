package org.phuchoang.ecp.shipping.internal.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Append-only carrier fact; {@code applied} says whether it advanced the shipment. */
public record TrackingEvent(UUID id, String carrierEventId, ShipmentStatus status, String description, String location,
        Instant carrierOccurredAt, boolean applied) {

    public TrackingEvent(UUID id, String carrierEventId, ShipmentStatus status, String description, String location,
            Instant carrierOccurredAt) {
        this(id, carrierEventId, status, description, location, carrierOccurredAt, false);
    }

    public TrackingEvent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(carrierOccurredAt, "carrierOccurredAt");
    }

    TrackingEvent applied(boolean value) {
        return new TrackingEvent(id, carrierEventId, status, description, location, carrierOccurredAt, value);
    }
}

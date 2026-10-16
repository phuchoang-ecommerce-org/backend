package org.phuchoang.ecp.shipping.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Stable shipment tracking view. */
public record ShipmentView(UUID id, UUID orderId, String carrier, String trackingReference, String status,
        Instant dispatchedAt, Instant deliveredAt, Instant lastUpdatedAt, List<TrackingEventView> events) {
    public ShipmentView { events = List.copyOf(events); }
    public record TrackingEventView(String carrierEventId, String status, String description, String location,
            Instant carrierOccurredAt, boolean applied) { }
}

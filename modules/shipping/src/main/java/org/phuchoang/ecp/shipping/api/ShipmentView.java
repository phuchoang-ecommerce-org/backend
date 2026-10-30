package org.phuchoang.ecp.shipping.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Stable shipment tracking view. */
public record ShipmentView(UUID id, UUID orderId, String carrier, String trackingReference, String status,
        Instant dispatchedAt, Instant deliveredAt, Instant createdAt, Instant lastUpdateReceivedAt,
        List<TrackingEventView> trackingEvents) {
    public ShipmentView { trackingEvents = List.copyOf(trackingEvents); }
    public record TrackingEventView(UUID id, String carrierEventId, String status, String description, String location,
            Instant carrierOccurredAt, Instant receivedAt, boolean applied) { }
}

package org.phuchoang.ecp.shipping.internal.application.tracking.query;

import org.phuchoang.ecp.shipping.internal.domain.model.Shipment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Application read model shared by shipment commands that return current tracking state. */
public record ShipmentTrackingView(
    UUID id,
    UUID orderId,
    String carrier,
    String trackingReference,
    String status,
    Instant dispatchedAt,
    Instant deliveredAt,
    Instant lastUpdatedAt,
    List<TrackingEventView> trackingEvents) {

  public static ShipmentTrackingView from(Shipment shipment) {
    Instant latest = shipment.trackingEvents().isEmpty() ? shipment.dispatchedAt()
        : shipment.trackingEvents().getLast().carrierOccurredAt();
    return new ShipmentTrackingView(shipment.id(), shipment.orderId(), shipment.carrier(), shipment.trackingReference(),
        shipment.status().name(), shipment.dispatchedAt(), shipment.deliveredAt(), latest,
        shipment.trackingEvents().stream().map(event -> new TrackingEventView(event.carrierEventId(),
            event.status().name(), event.description(), event.location(), event.carrierOccurredAt(), event.applied()))
            .toList());
  }

  public record TrackingEventView(
      String carrierEventId,
      String status,
      String description,
      String location,
      Instant occurredAt,
      boolean applied) {
  }
}

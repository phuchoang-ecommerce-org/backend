package org.phuchoang.ecp.shipping.internal.application.tracking.query;

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
    Instant createdAt,
    Instant lastUpdateReceivedAt,
    List<TrackingEventView> trackingEvents) {

  public record TrackingEventView(
      UUID id,
      String carrierEventId,
      String status,
      String description,
      String location,
      Instant carrierOccurredAt,
      Instant receivedAt,
      boolean applied) {
  }
}

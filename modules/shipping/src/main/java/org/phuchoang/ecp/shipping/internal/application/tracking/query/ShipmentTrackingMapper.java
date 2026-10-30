package org.phuchoang.ecp.shipping.internal.application.tracking.query;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.shipping.internal.domain.model.Shipment;
import org.phuchoang.ecp.shipping.internal.domain.model.TrackingEvent;

import java.time.Instant;

/** Maps the Shipment aggregate into the application-owned tracking projection. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ShipmentTrackingMapper {
  @Mapping(target = "lastUpdateReceivedAt", expression = "java(lastUpdateReceivedAt(shipment))")
  ShipmentTrackingView trackingView(Shipment shipment);

  ShipmentTrackingView.TrackingEventView trackingEventView(TrackingEvent event);

  default Instant lastUpdateReceivedAt(Shipment shipment) {
    return shipment.trackingEvents().stream().map(TrackingEvent::receivedAt).max(Instant::compareTo).orElse(null);
  }
}

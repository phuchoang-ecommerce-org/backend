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
  @Mapping(target = "lastUpdatedAt", expression = "java(lastUpdatedAt(shipment))")
  ShipmentTrackingView trackingView(Shipment shipment);

  @Mapping(target = "occurredAt", source = "carrierOccurredAt")
  ShipmentTrackingView.TrackingEventView trackingEventView(TrackingEvent event);

  default Instant lastUpdatedAt(Shipment shipment) {
    return shipment.trackingEvents().isEmpty() ? shipment.dispatchedAt()
        : shipment.trackingEvents().getLast().carrierOccurredAt();
  }
}

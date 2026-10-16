package org.phuchoang.ecp.shipping.api;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.shipping.internal.application.tracking.command.ReceiveCarrierEventCommand;
import org.phuchoang.ecp.shipping.internal.application.tracking.query.ShipmentTrackingView;

/** Maps Shipping's published contract to its application-use-case contracts. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface ShippingApiMapper {
  ReceiveCarrierEventCommand command(ShippingFacade.CarrierEvent event);

  @Mapping(target = "events", source = "trackingEvents")
  ShipmentView shipmentView(ShipmentTrackingView view);

  @Mapping(target = "carrierOccurredAt", source = "occurredAt")
  ShipmentView.TrackingEventView trackingEventView(ShipmentTrackingView.TrackingEventView view);
}

package org.phuchoang.ecp.shipping.internal.infrastructure.persistence;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.shipping.internal.domain.model.Shipment;
import org.phuchoang.ecp.shipping.internal.domain.model.TrackingEvent;

import java.util.List;

/** Maps framework-free shipment state to and from JDBC-only row representations. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface ShipmentPersistenceMapper {
  @Mapping(target = "statusRank", expression = "java(shipment.status().rank())")
  ShipmentEntity entity(Shipment shipment);

  @Mapping(target = "record", ignore = true)
  @Mapping(target = "save", ignore = true)
  Shipment domain(ShipmentEntity entity, List<TrackingEvent> trackingEvents);

  @Mapping(target = "shipmentId", ignore = true)
  @Mapping(target = "statusRank", expression = "java(event.status().rank())")
  TrackingEventEntity entity(TrackingEvent event);

  TrackingEvent domain(TrackingEventEntity entity);
}

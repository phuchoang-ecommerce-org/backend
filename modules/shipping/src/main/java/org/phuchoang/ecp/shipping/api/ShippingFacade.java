package org.phuchoang.ecp.shipping.api;

import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Published Shipping boundary. Web adapters and Ordering never see Shipping
 * internals.
 */
public interface ShippingFacade {
  ShipmentView createShipment(IdentityActor caller, UUID orderId, String carrier, String postalCode, String countryCode,
      UUID correlationId);

  ShipmentView getShipment(IdentityActor caller, UUID shipmentId);

  List<ShipmentView> listShipments(IdentityActor caller, UUID orderId);

  ShipmentView receiveCarrierEvent(CarrierEvent event, String signature, UUID correlationId);

  ShipmentView confirmShipmentDelivery(UUID shipmentId, boolean disputed, UUID correlationId);

  record CarrierEvent(String carrier, String trackingReference, String carrierEventId, String status,
      String description, String location, Instant occurredAt) {
  }
}

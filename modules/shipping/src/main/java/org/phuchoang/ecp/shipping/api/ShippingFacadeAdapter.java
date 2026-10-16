package org.phuchoang.ecp.shipping.api;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.shipping.internal.application.create.command.CreateShipmentService;
import org.phuchoang.ecp.shipping.internal.application.tracking.command.ConfirmShipmentDeliveryService;
import org.phuchoang.ecp.shipping.internal.application.tracking.command.ReceiveCarrierEventCommand;
import org.phuchoang.ecp.shipping.internal.application.tracking.command.ReceiveCarrierEventService;
import org.phuchoang.ecp.shipping.internal.application.tracking.query.ShipmentTrackingQueryService;
import org.phuchoang.ecp.shipping.internal.application.tracking.query.ShipmentTrackingView;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class ShippingFacadeAdapter implements ShippingFacade {
  private final CreateShipmentService create;
  private final ReceiveCarrierEventService receive;
  private final ConfirmShipmentDeliveryService confirm;
  private final ShipmentTrackingQueryService tracking;
  private final IdentityAuthorization authorization;

  @Override
  public ShipmentView createShipment(IdentityActor caller, UUID orderId, String carrier, String postalCode,
      String countryCode, UUID correlationId) {
    return view(create.create(caller, orderId, carrier, postalCode, countryCode, correlationId));
  }

  @Override
  public ShipmentView getShipment(IdentityActor caller, UUID shipmentId) {
    authorization.assertAuthorized(caller, "trackOrder");
    return view(tracking.get(shipmentId));
  }

  @Override
  public List<ShipmentView> listShipments(IdentityActor caller, UUID orderId) {
    authorization.assertAuthorized(caller, "trackOrder");
    return tracking.list(orderId).stream().map(ShippingFacadeAdapter::view).toList();
  }

  @Override
  public ShipmentView receiveCarrierEvent(CarrierEvent event, String signature, UUID correlationId) {
    return view(receive.receive(new ReceiveCarrierEventCommand(event.carrier(), event.trackingReference(),
        event.carrierEventId(), event.status(), event.description(), event.location(), event.occurredAt()), signature,
        correlationId));
  }

  @Override
  public ShipmentView confirmShipmentDelivery(UUID shipmentId, boolean disputed, UUID correlationId) {
    return view(confirm.confirm(shipmentId, disputed, correlationId));
  }

  private static ShipmentView view(ShipmentTrackingView value) {
    return new ShipmentView(value.id(), value.orderId(), value.carrier(), value.trackingReference(), value.status(),
        value.dispatchedAt(), value.deliveredAt(), value.lastUpdatedAt(),
        value.trackingEvents().stream().map(event -> new ShipmentView.TrackingEventView(event.carrierEventId(),
            event.status(), event.description(), event.location(), event.occurredAt(), event.applied())).toList());
  }
}

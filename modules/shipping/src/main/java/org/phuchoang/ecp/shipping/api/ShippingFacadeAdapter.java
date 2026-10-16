package org.phuchoang.ecp.shipping.api;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.shipping.internal.application.create.command.CreateShipmentService;
import org.phuchoang.ecp.shipping.internal.application.tracking.command.ConfirmShipmentDeliveryService;
import org.phuchoang.ecp.shipping.internal.application.tracking.command.ReceiveCarrierEventService;
import org.phuchoang.ecp.shipping.internal.application.tracking.query.ShipmentTrackingQueryService;
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
  private final ShippingApiMapper mapper;

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
    return tracking.list(orderId).stream().map(mapper::shipmentView).toList();
  }

  @Override
  public ShipmentView receiveCarrierEvent(CarrierEvent event, String signature, UUID correlationId) {
    return mapper.shipmentView(receive.receive(mapper.command(event), signature, correlationId));
  }

  @Override
  public ShipmentView confirmShipmentDelivery(UUID shipmentId, boolean disputed, UUID correlationId) {
    return view(confirm.confirm(shipmentId, disputed, correlationId));
  }

  private ShipmentView view(org.phuchoang.ecp.shipping.internal.application.tracking.query.ShipmentTrackingView view) {
    return mapper.shipmentView(view);
  }
}

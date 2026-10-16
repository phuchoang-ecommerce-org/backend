package org.phuchoang.ecp.shipping.internal.application.create.command;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.ordering.api.OrderFulfilmentFacade;
import org.phuchoang.ecp.shipping.internal.application.carrier.CarrierGateway;
import org.phuchoang.ecp.shipping.internal.application.tracking.query.ShipmentTrackingView;
import org.phuchoang.ecp.shipping.internal.domain.model.Shipment;
import org.phuchoang.ecp.shipping.internal.domain.service.ShipmentDomainService;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

/**
 * UC-SHP-03: book externally first; only a confirmed booking becomes a
 * Shipment.
 */
@Service
@RequiredArgsConstructor
public class CreateShipmentService {
  private final IdentityAuthorization authorization;
  private final OrderFulfilmentFacade orders;
  private final CarrierGateway carrier;
  private final ShipmentDomainService shipments;
  private final Clock clock;

  public ShipmentTrackingView create(IdentityActor caller, UUID orderId, String carrierName, String postalCode, String countryCode,
      UUID correlationId) {
    authorization.assertAuthorized(caller, "createShipment");
    if (!"PACKED".equals(orders.getOrderForFulfilment(orderId).status()))
      throw new IllegalStateException("Order must be Packed before dispatch.");
    CarrierGateway.Booking booking = carrier.book(orderId, carrierName, postalCode, countryCode);
    if (!booking.accepted())
      throw new CarrierDeclined(booking.rejectionReason());
    Shipment shipment = shipments.create(orderId, carrierName, booking.trackingReference(), postalCode, countryCode,
        clock.instant());
    orders.markDispatched(orderId, correlationId);
    return ShipmentTrackingView.from(shipment);
  }

  public static final class CarrierDeclined extends RuntimeException {
    public CarrierDeclined(String message) {
      super(message);
    }
  }
}

package org.phuchoang.ecp.shipping.internal.application.tracking.command;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.ordering.api.OrderFulfilmentFacade;
import org.phuchoang.ecp.shipping.internal.domain.model.Shipment;
import org.phuchoang.ecp.shipping.internal.domain.service.ShipmentDomainService;
import org.phuchoang.ecp.shipping.internal.application.tracking.query.ShipmentTrackingView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

/**
 * Delivery moves Ordering only through its lifecycle API; it intentionally
 * makes no payment claim.
 */
@Service
@RequiredArgsConstructor
public class ConfirmShipmentDeliveryService {
  private final ShipmentDomainService shipments;
  private final OrderFulfilmentFacade orders;

  @Transactional
  public ShipmentTrackingView confirm(UUID shipmentId, boolean disputed, UUID correlationId) {
    Shipment shipment = shipments.require(shipmentId);
    if (!"SHIPPING".equals(orders.getOrderForFulfilment(shipment.orderId()).status()))
      throw new IllegalStateException("Order must be Shipping before delivery confirmation.");
    orders.markDelivered(shipment.orderId(), correlationId);
    return ShipmentTrackingView.from(shipment);
  }
}

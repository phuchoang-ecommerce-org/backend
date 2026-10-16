package org.phuchoang.ecp.shipping.internal.domain.service;

import org.phuchoang.ecp.shipping.internal.domain.model.Shipment;
import org.phuchoang.ecp.shipping.internal.domain.model.TrackingEvent;
import org.phuchoang.ecp.shipping.internal.domain.repository.ShipmentRepository;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain entry point: it loads/persists the complete Shipment aggregate, never
 * tracking rows alone.
 */
public final class ShipmentDomainService {
  private final ShipmentRepository shipments;

  public ShipmentDomainService(ShipmentRepository shipments) {
    this.shipments = shipments;
  }

  public Shipment create(UUID orderId, String carrier, String trackingReference, String postalCode, String countryCode,
      Instant dispatchedAt) {
    return Shipment.dispatch(orderId, carrier, trackingReference, postalCode, countryCode, dispatchedAt)
        .save(shipments);
  }

  public Shipment record(UUID shipmentId, TrackingEvent event) {
    Shipment current = require(shipmentId);
    return current.record(event).save(shipments);
  }

  public Shipment require(UUID shipmentId) {
    return shipments.findById(shipmentId).orElseThrow(() -> new ShipmentNotFound(shipmentId));
  }

  public Shipment require(String carrier, String trackingReference) {
    return shipments.findByCarrierAndTrackingReference(carrier, trackingReference)
        .orElseThrow(() -> new ShipmentNotFound(trackingReference));
  }

  public static final class ShipmentNotFound extends RuntimeException {
    public ShipmentNotFound(Object id) {
      super("Shipment not found: " + id);
    }
  }
}

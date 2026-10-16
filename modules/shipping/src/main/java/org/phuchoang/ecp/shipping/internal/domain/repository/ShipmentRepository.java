package org.phuchoang.ecp.shipping.internal.domain.repository;

import org.jmolecules.ddd.annotation.Repository;
import org.phuchoang.ecp.shipping.internal.domain.model.Shipment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Authoritative persistence port for the Shipment aggregate and its tracking
 * history.
 */
@Repository
public interface ShipmentRepository {
  Shipment save(Shipment shipment);

  Optional<Shipment> findById(UUID id);

  Optional<Shipment> findByCarrierAndTrackingReference(String carrier, String trackingReference);

  List<Shipment> findByOrderId(UUID orderId);
}

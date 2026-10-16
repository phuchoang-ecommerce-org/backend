package org.phuchoang.ecp.shipping.internal.application.tracking.query;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.shipping.internal.domain.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

/** Observational tracking query boundary. */
@Service
@RequiredArgsConstructor
public class ShipmentTrackingQueryService {
  private final ShipmentRepository shipments;
  private final ShipmentTrackingMapper mapper;

  @Transactional(readOnly = true)
  public ShipmentTrackingView get(UUID id) {
    return mapper.trackingView(shipments.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Shipment not found.")));
  }

  @Transactional(readOnly = true)
  public List<ShipmentTrackingView> list(UUID orderId) {
    return shipments.findByOrderId(orderId).stream().map(mapper::trackingView).toList();
  }
}

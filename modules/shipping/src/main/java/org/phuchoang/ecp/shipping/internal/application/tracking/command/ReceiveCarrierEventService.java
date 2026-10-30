package org.phuchoang.ecp.shipping.internal.application.tracking.command;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.audit.api.AuditRecord;
import org.phuchoang.ecp.audit.api.AuditTrail;
import org.phuchoang.ecp.shipping.internal.domain.model.ShipmentStatus;
import org.phuchoang.ecp.shipping.internal.domain.model.TrackingEvent;
import org.phuchoang.ecp.shipping.internal.domain.service.ShipmentDomainService;
import org.phuchoang.ecp.shipping.internal.application.tracking.query.ShipmentTrackingMapper;
import org.phuchoang.ecp.shipping.internal.application.tracking.query.ShipmentTrackingView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;

/**
 * System-only callback path. Verification is deliberately the first observable
 * action.
 */
@Service
@RequiredArgsConstructor
public class ReceiveCarrierEventService {
  private final CarrierEventAuthenticator authenticator;
  private final ShipmentDomainService shipments;
  private final ShipmentTrackingMapper trackingMapper;
  private final AuditTrail audit;
  private final Clock clock;

  @Transactional
  public ShipmentTrackingView receive(ReceiveCarrierEventCommand event, String signature, UUID correlationId) {
    if (!authenticator.authentic(event, signature)) {
      audit.record(
          new AuditRecord(UUID.randomUUID(), null, null, "CARRIER_EVENT_SIGNATURE_REJECTED", "CarrierEvent", null,
              "shipping", Map.of(), Map.of("carrier", event.carrier()), "Carrier callback authenticity failed",
              correlationId, clock.instant(), "carrier-webhook", true));
      throw new SecurityException("Carrier event authenticity verification failed.");
    }
    return trackingMapper.trackingView(shipments.record(shipments.require(event.carrier(), event.trackingReference()).id(),
        new TrackingEvent(UUID.randomUUID(),
            event.carrierEventId(), ShipmentStatus.valueOf(event.status()), event.description(), event.location(),
            event.occurredAt(), clock.instant())));
  }
}

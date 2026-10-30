package org.phuchoang.ecp.shipping.api;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.phuchoang.ecp.shipping.internal.application.tracking.query.ShipmentTrackingView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ShippingApiMapperTest {
  private final ShippingApiMapper mapper = Mappers.getMapper(ShippingApiMapper.class);

  @Test
  void mapsCarrierEventAndTrackingProjectionAtThePublicBoundary() {
    ShippingFacade.CarrierEvent event = new ShippingFacade.CarrierEvent("ACME", "TRK-1", "carrier-1", "IN_TRANSIT",
        "Collected", "Ho Chi Minh City", Instant.parse("2026-10-16T09:00:00Z"));
    UUID shipmentId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    ShipmentTrackingView tracking = new ShipmentTrackingView(shipmentId, UUID.randomUUID(), "ACME", "TRK-1",
        "IN_TRANSIT", Instant.parse("2026-10-16T08:00:00Z"), null, Instant.parse("2026-10-16T08:00:00Z"),
        Instant.parse("2026-10-16T09:05:00Z"), List.of(new ShipmentTrackingView.TrackingEventView(eventId,
            "carrier-1", "IN_TRANSIT", "Collected", "Ho Chi Minh City", Instant.parse("2026-10-16T09:00:00Z"),
            Instant.parse("2026-10-16T09:05:00Z"), true)));

    var command = mapper.command(event);
    assertThat(command.carrier()).isEqualTo("ACME");
    assertThat(command.trackingReference()).isEqualTo("TRK-1");
    assertThat(command.carrierEventId()).isEqualTo("carrier-1");
    assertThat(command.occurredAt()).isEqualTo(Instant.parse("2026-10-16T09:00:00Z"));
    ShipmentView shipment = mapper.shipmentView(tracking);
    assertThat(shipment.id()).isEqualTo(shipmentId);
    assertThat(shipment.trackingEvents()).containsExactly(new ShipmentView.TrackingEventView(eventId, "carrier-1",
        "IN_TRANSIT", "Collected", "Ho Chi Minh City", Instant.parse("2026-10-16T09:00:00Z"),
        Instant.parse("2026-10-16T09:05:00Z"), true));
  }
}

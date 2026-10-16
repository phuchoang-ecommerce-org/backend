package org.phuchoang.ecp.shipping.internal.application.tracking.query;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.phuchoang.ecp.shipping.internal.domain.model.Shipment;
import org.phuchoang.ecp.shipping.internal.domain.model.ShipmentStatus;
import org.phuchoang.ecp.shipping.internal.domain.model.TrackingEvent;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ShipmentTrackingMapperTest {
  private final ShipmentTrackingMapper mapper = Mappers.getMapper(ShipmentTrackingMapper.class);

  @Test
  void mapsAggregateStateAndLatestCarrierFactToTheTrackingProjection() {
    Shipment shipment = Shipment.dispatch(UUID.randomUUID(), "ACME", "TRK-1", "70000", "VN",
        Instant.parse("2026-10-16T08:00:00Z"))
        .record(new TrackingEvent(UUID.randomUUID(), "carrier-1", ShipmentStatus.IN_TRANSIT, "Collected",
            "Ho Chi Minh City", Instant.parse("2026-10-16T09:00:00Z")));

    ShipmentTrackingView view = mapper.trackingView(shipment);

    assertThat(view.lastUpdatedAt()).isEqualTo(Instant.parse("2026-10-16T09:00:00Z"));
    assertThat(view.trackingEvents()).containsExactly(new ShipmentTrackingView.TrackingEventView("carrier-1",
        "IN_TRANSIT", "Collected", "Ho Chi Minh City", Instant.parse("2026-10-16T09:00:00Z"), true));
  }
}

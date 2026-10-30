package org.phuchoang.ecp.shipping.internal.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.phuchoang.ecp.shipping.internal.domain.model.Shipment;
import org.phuchoang.ecp.shipping.internal.domain.model.ShipmentStatus;
import org.phuchoang.ecp.shipping.internal.domain.model.TrackingEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ShipmentPersistenceMapperTest {
  private final ShipmentPersistenceMapper mapper = Mappers.getMapper(ShipmentPersistenceMapper.class);

  @Test
  void mapsTheAggregateAndAppendOnlyFactsToJdbcEntitiesAndBack() {
    Shipment shipment = Shipment.dispatch(UUID.randomUUID(), "ACME", "TRK-1", "70000", "VN",
        Instant.parse("2026-10-16T08:00:00Z"))
        .record(new TrackingEvent(UUID.randomUUID(), "carrier-1", ShipmentStatus.IN_TRANSIT, "Collected",
            "Ho Chi Minh City", Instant.parse("2026-10-16T09:00:00Z"), Instant.parse("2026-10-16T09:05:00Z")));

    ShipmentEntity entity = mapper.entity(shipment);
    TrackingEvent trackingEvent = shipment.trackingEvents().getFirst();
    TrackingEventEntity trackingEntity = mapper.entity(trackingEvent);
    Shipment restored = mapper.domain(entity, List.of(mapper.domain(trackingEntity)));

    assertThat(entity.status()).isEqualTo("IN_TRANSIT");
    assertThat(entity.statusRank()).isEqualTo(ShipmentStatus.IN_TRANSIT.rank());
    assertThat(trackingEntity.statusRank()).isEqualTo(ShipmentStatus.IN_TRANSIT.rank());
    assertThat(restored).isEqualTo(shipment);
  }
}

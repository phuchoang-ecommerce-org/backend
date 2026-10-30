package org.phuchoang.ecp.shipping.internal.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ShipmentTest {

    @Test
    void retainsAnOutOfOrderCarrierUpdateWithoutMovingADeliveredShipmentBackward() {
        Shipment shipment = Shipment.dispatch(UUID.randomUUID(), "ACME", "TRK-1", "70000", "VN", Instant.parse("2026-10-16T08:00:00Z"));
        shipment = shipment.record(new TrackingEvent(UUID.randomUUID(), "carrier-delivered", ShipmentStatus.DELIVERED,
            "Delivered", "Ho Chi Minh City", Instant.parse("2026-10-17T08:00:00Z"), Instant.parse("2026-10-17T08:05:00Z")));

        Shipment afterLateUpdate = shipment.record(new TrackingEvent(UUID.randomUUID(), "carrier-in-transit", ShipmentStatus.IN_TRANSIT,
            "Late scan", "Da Nang", Instant.parse("2026-10-16T12:00:00Z"), Instant.parse("2026-10-17T08:06:00Z")));

        assertThat(afterLateUpdate.status()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(afterLateUpdate.trackingEvents()).hasSize(2);
        assertThat(afterLateUpdate.trackingEvents().getLast().applied()).isFalse();
    }

    @Test
    void treatsARepeatedCarrierEventAsOneBusinessEffect() {
        Shipment shipment = Shipment.dispatch(UUID.randomUUID(), "ACME", "TRK-1", "70000", "VN", Instant.parse("2026-10-16T08:00:00Z"));
        TrackingEvent event = new TrackingEvent(UUID.randomUUID(), "carrier-1", ShipmentStatus.IN_TRANSIT,
            "Collected", "Ho Chi Minh City", Instant.parse("2026-10-16T09:00:00Z"), Instant.parse("2026-10-16T09:05:00Z"));

        Shipment once = shipment.record(event);
        Shipment twice = once.record(event);

        assertThat(twice).isEqualTo(once);
        assertThat(twice.trackingEvents()).hasSize(1);
    }
}

package org.phuchoang.ecp.shipping.internal.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.shipping.internal.domain.model.Shipment;
import org.phuchoang.ecp.shipping.internal.domain.model.ShipmentStatus;
import org.phuchoang.ecp.shipping.internal.domain.model.TrackingEvent;
import org.phuchoang.ecp.shipping.internal.domain.repository.ShipmentRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PostgreSQL adapter; tracking facts are insert-only and the root uses
 * optimistic locking.
 */
@Repository
@RequiredArgsConstructor
class JdbcShipmentRepository implements ShipmentRepository {
  private final JdbcClient jdbc;

  @Override
  public Shipment save(Shipment shipment) {
    int changed = jdbc.sql("""
        INSERT INTO shipping_shipment (id, order_id, carrier, tracking_reference, status, status_rank,
          destination_postal_code, destination_country_code, dispatched_at, delivered_at, version)
        VALUES (:id,:orderId,:carrier,:tracking,:status,:rank,:postal,:country,:dispatched,:delivered,0)
        ON CONFLICT (id) DO UPDATE SET status=:status,status_rank=:rank,delivered_at=:delivered,
          updated_at=now(),version=shipping_shipment.version+1
        WHERE shipping_shipment.version=:version
        """).param("id", shipment.id()).param("orderId", shipment.orderId()).param("carrier", shipment.carrier())
        .param("tracking", shipment.trackingReference()).param("status", shipment.status().name())
        .param("rank", shipment.status().rank()).param("postal", shipment.destinationPostalCode())
        .param("country", shipment.destinationCountryCode()).param("dispatched", shipment.dispatchedAt())
        .param("delivered", shipment.deliveredAt()).param("version", shipment.version()).update();
    if (changed != 1)
      throw new IllegalStateException("Shipment changed concurrently; retry the carrier event.");
    for (TrackingEvent event : shipment.trackingEvents())
      jdbc.sql(
          """
              INSERT INTO shipping_tracking_event (id,shipment_id,carrier_event_id,status,status_rank,description,location,
                carrier_occurred_at,applied) VALUES (:id,:shipment,:carrierEvent,:status,:rank,:description,:location,:occurred,:applied)
              ON CONFLICT (shipment_id,carrier_event_id) WHERE carrier_event_id IS NOT NULL DO NOTHING
              """)
          .param("id", event.id()).param("shipment", shipment.id()).param("carrierEvent", event.carrierEventId())
          .param("status", event.status().name()).param("rank", event.status().rank())
          .param("description", event.description())
          .param("location", event.location()).param("occurred", event.carrierOccurredAt())
          .param("applied", event.applied()).update();
    return new Shipment(shipment.id(), shipment.orderId(), shipment.carrier(), shipment.trackingReference(),
        shipment.destinationPostalCode(), shipment.destinationCountryCode(), shipment.status(), shipment.dispatchedAt(),
        shipment.deliveredAt(), shipment.version() + 1, shipment.trackingEvents());
  }

  @Override
  public Optional<Shipment> findById(UUID id) {
    return find("id = :value", id);
  }

  @Override
  public Optional<Shipment> findByCarrierAndTrackingReference(String carrier, String trackingReference) {
    return jdbc.sql("SELECT * FROM shipping_shipment WHERE carrier=:carrier AND tracking_reference=:tracking")
        .param("carrier", carrier).param("tracking", trackingReference)
        .query((rs, row) -> shipment(rs.getObject("id", UUID.class), rs)).optional();
  }

  @Override
  public List<Shipment> findByOrderId(UUID orderId) {
    return jdbc.sql("SELECT * FROM shipping_shipment WHERE order_id=:id ORDER BY created_at").param("id", orderId)
        .query((rs, row) -> shipment(rs.getObject("id", UUID.class), rs)).list();
  }

  private Optional<Shipment> find(String condition, UUID value) {
    return jdbc.sql("SELECT * FROM shipping_shipment WHERE " + condition).param("value", value)
        .query((rs, row) -> shipment(value, rs)).optional();
  }

  private Shipment shipment(UUID id, java.sql.ResultSet rs) throws java.sql.SQLException {
    return new Shipment(id, rs.getObject("order_id", UUID.class), rs.getString("carrier"),
        rs.getString("tracking_reference"),
        rs.getString("destination_postal_code"), rs.getString("destination_country_code"),
        ShipmentStatus.valueOf(rs.getString("status")),
        instant(rs.getTimestamp("dispatched_at")), instant(rs.getTimestamp("delivered_at")), rs.getLong("version"),
        events(id));
  }

  private List<TrackingEvent> events(UUID shipmentId) {
    return jdbc.sql("SELECT * FROM shipping_tracking_event WHERE shipment_id=:id ORDER BY carrier_occurred_at, id")
        .param("id", shipmentId)
        .query((rs, row) -> new TrackingEvent(rs.getObject("id", UUID.class), rs.getString("carrier_event_id"),
            ShipmentStatus.valueOf(rs.getString("status")), rs.getString("description"), rs.getString("location"),
            rs.getTimestamp("carrier_occurred_at").toInstant(), rs.getBoolean("applied")))
        .list();
  }

  private static java.time.Instant instant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }
}

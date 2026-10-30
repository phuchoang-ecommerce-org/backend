package org.phuchoang.ecp.shipping.internal.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.shipping.internal.domain.model.Shipment;
import org.phuchoang.ecp.shipping.internal.domain.model.TrackingEvent;
import org.phuchoang.ecp.shipping.internal.domain.repository.ShipmentRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
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
  private final ShipmentPersistenceMapper mapper;

  @Override
  public Shipment save(Shipment shipment) {
    ShipmentEntity entity = mapper.entity(shipment);
    int changed = jdbc.sql("""
        INSERT INTO shipping_shipment (id, order_id, carrier, tracking_reference, status, status_rank,
          destination_postal_code, destination_country_code, dispatched_at, delivered_at, created_at, version)
        VALUES (:id,:orderId,:carrier,:tracking,:status,:rank,:postal,:country,:dispatched,:delivered,:created,0)
        ON CONFLICT (id) DO UPDATE SET status=:status,status_rank=:rank,delivered_at=:delivered,
          updated_at=now(),version=shipping_shipment.version+1
        WHERE shipping_shipment.version=:version
        """).param("id", entity.id()).param("orderId", entity.orderId()).param("carrier", entity.carrier())
        .param("tracking", entity.trackingReference()).param("status", entity.status())
        .param("rank", entity.statusRank()).param("postal", entity.destinationPostalCode())
        .param("country", entity.destinationCountryCode()).param("dispatched", timestamp(entity.dispatchedAt()))
        .param("delivered", timestamp(entity.deliveredAt())).param("created", timestamp(entity.createdAt()))
        .param("version", entity.version()).update();
    if (changed != 1)
      throw new IllegalStateException("Shipment changed concurrently; retry the carrier event.");
    for (TrackingEvent event : shipment.trackingEvents()) {
      TrackingEventEntity trackingEvent = mapper.entity(event);
      jdbc.sql(
          """
              INSERT INTO shipping_tracking_event (id,shipment_id,carrier_event_id,status,status_rank,description,location,
                carrier_occurred_at,received_at,applied) VALUES (:id,:shipment,:carrierEvent,:status,:rank,:description,:location,:occurred,:received,:applied)
              ON CONFLICT (shipment_id,carrier_event_id) WHERE carrier_event_id IS NOT NULL DO NOTHING
              """)
          .param("id", trackingEvent.id()).param("shipment", shipment.id())
          .param("carrierEvent", trackingEvent.carrierEventId())
          .param("status", trackingEvent.status()).param("rank", trackingEvent.statusRank())
          .param("description", trackingEvent.description())
          .param("location", trackingEvent.location()).param("occurred", timestamp(trackingEvent.carrierOccurredAt()))
          .param("received", timestamp(trackingEvent.receivedAt()))
          .param("applied", trackingEvent.applied()).update();
    }
    return mapper.domain(entity.withVersion(shipment.version() + 1), shipment.trackingEvents());
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
    return mapper.domain(shipmentEntity(id, rs), events(id));
  }

  private List<TrackingEvent> events(UUID shipmentId) {
    return jdbc.sql("SELECT * FROM shipping_tracking_event WHERE shipment_id=:id ORDER BY carrier_occurred_at, id")
        .param("id", shipmentId)
        .query((rs, row) -> mapper.domain(new TrackingEventEntity(rs.getObject("id", UUID.class), shipmentId,
            rs.getString("carrier_event_id"), rs.getString("status"), rs.getInt("status_rank"),
            rs.getString("description"), rs.getString("location"), rs.getTimestamp("carrier_occurred_at").toInstant(),
            rs.getTimestamp("received_at").toInstant(), rs.getBoolean("applied"))))
        .list();
  }

  private static ShipmentEntity shipmentEntity(UUID id, java.sql.ResultSet rs) throws java.sql.SQLException {
    return new ShipmentEntity(id, rs.getObject("order_id", UUID.class), rs.getString("carrier"),
        rs.getString("tracking_reference"), rs.getString("destination_postal_code"),
        rs.getString("destination_country_code"), rs.getString("status"), rs.getInt("status_rank"),
        instant(rs.getTimestamp("dispatched_at")), instant(rs.getTimestamp("delivered_at")),
        instant(rs.getTimestamp("created_at")), rs.getLong("version"));
  }

  private static java.time.Instant instant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }

  private static Timestamp timestamp(Instant instant) {
    return instant == null ? null : Timestamp.from(instant);
  }
}

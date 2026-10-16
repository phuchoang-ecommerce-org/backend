package org.phuchoang.ecp.shipping.internal.domain.model;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.phuchoang.ecp.shipping.internal.domain.repository.ShipmentRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The fulfilment aggregate. It never lets a late carrier fact move a parcel
 * backwards.
 */
@AggregateRoot
public record Shipment(@Identity UUID id, UUID orderId, String carrier, String trackingReference,
    String destinationPostalCode, String destinationCountryCode, ShipmentStatus status, Instant dispatchedAt,
    Instant deliveredAt, long version, List<TrackingEvent> trackingEvents) {

  public Shipment {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(orderId, "orderId");
    Objects.requireNonNull(carrier, "carrier");
    Objects.requireNonNull(status, "status");
    trackingEvents = List.copyOf(trackingEvents);
  }

  public static Shipment dispatch(UUID orderId, String carrier, String trackingReference, String postalCode,
      String countryCode, Instant dispatchedAt) {
    return new Shipment(UUID.randomUUID(), orderId, carrier, trackingReference, postalCode, countryCode,
        ShipmentStatus.DISPATCHED, Objects.requireNonNull(dispatchedAt), null, 0, List.of());
  }

  /**
   * Applies each carrier fact once and retains late facts for the complete trace.
   */
  public Shipment record(TrackingEvent incoming) {
    Objects.requireNonNull(incoming, "incoming");
    if (incoming.carrierEventId() != null && trackingEvents.stream()
        .anyMatch(event -> incoming.carrierEventId().equals(event.carrierEventId())))
      return this;
    boolean applies = incoming.status().rank() >= status.rank();
    TrackingEvent stored = incoming.applied(applies);
    List<TrackingEvent> history = new ArrayList<>(trackingEvents);
    history.add(stored);
    ShipmentStatus nextStatus = applies ? incoming.status() : status;
    Instant nextDeliveredAt = nextStatus == ShipmentStatus.DELIVERED && deliveredAt == null
        ? incoming.carrierOccurredAt()
        : deliveredAt;
    return new Shipment(id, orderId, carrier, trackingReference, destinationPostalCode, destinationCountryCode,
        nextStatus, dispatchedAt, nextDeliveredAt, version, history);
  }

  /**
   * Persists the aggregate and its append-only child facts as one write boundary.
   */
  public Shipment save(ShipmentRepository repository) {
    return repository.save(this);
  }
}

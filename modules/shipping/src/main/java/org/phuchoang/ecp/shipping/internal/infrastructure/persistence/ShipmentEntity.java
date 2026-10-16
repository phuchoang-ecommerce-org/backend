package org.phuchoang.ecp.shipping.internal.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

/** JDBC representation of the shipment root row. */
record ShipmentEntity(
    UUID id,
    UUID orderId,
    String carrier,
    String trackingReference,
    String destinationPostalCode,
    String destinationCountryCode,
    String status,
    int statusRank,
    Instant dispatchedAt,
    Instant deliveredAt,
    long version) {

  ShipmentEntity withVersion(long nextVersion) {
    return new ShipmentEntity(id, orderId, carrier, trackingReference, destinationPostalCode, destinationCountryCode,
        status, statusRank, dispatchedAt, deliveredAt, nextVersion);
  }
}

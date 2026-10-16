package org.phuchoang.ecp.shipping.internal.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

/** JDBC representation of an append-only carrier tracking-event row. */
record TrackingEventEntity(
    UUID id,
    UUID shipmentId,
    String carrierEventId,
    String status,
    int statusRank,
    String description,
    String location,
    Instant carrierOccurredAt,
    boolean applied) {
}

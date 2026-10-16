package org.phuchoang.ecp.shipping.internal.application.tracking.command;

import java.time.Instant;

/** Application input for the authenticated carrier-event use case. */
public record ReceiveCarrierEventCommand(
    String carrier,
    String trackingReference,
    String carrierEventId,
    String status,
    String description,
    String location,
    Instant occurredAt) {
}

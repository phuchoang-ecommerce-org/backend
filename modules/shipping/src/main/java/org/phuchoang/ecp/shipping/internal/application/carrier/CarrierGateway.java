package org.phuchoang.ecp.shipping.internal.application.carrier;

import java.util.UUID;

/** Application port for a carrier booking; a rejection/unavailability creates no shipment. */
public interface CarrierGateway {
    Booking book(UUID orderId, String carrier, String postalCode, String countryCode);
    record Booking(boolean accepted, String trackingReference, String rejectionReason) {
        public static Booking rejected(String reason) { return new Booking(false, null, reason); }
        public static Booking accepted(String trackingReference) { return new Booking(true, trackingReference, null); }
    }
}

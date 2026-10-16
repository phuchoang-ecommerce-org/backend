package org.phuchoang.ecp.shipping.internal.infrastructure.carrier;

import org.phuchoang.ecp.shipping.internal.application.carrier.CarrierGateway;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import java.util.UUID;

/**
 * Safe local adapter; production replaces this port with a carrier-specific
 * adapter.
 */
@Component
@Primary
class DevelopmentCarrierGateway implements CarrierGateway {
  @Override
  public Booking book(UUID orderId, String carrier, String postalCode, String countryCode) {
    if (carrier == null || carrier.isBlank() || countryCode == null || countryCode.isBlank())
      return Booking.rejected("Carrier or destination is unavailable.");
    return Booking.accepted("DEV-" + UUID.randomUUID());
  }
}

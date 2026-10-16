package org.phuchoang.ecp.shipping.internal.infrastructure.carrier;

import org.phuchoang.ecp.shipping.internal.application.tracking.command.CarrierEventAuthenticator;
import org.phuchoang.ecp.shipping.internal.application.tracking.command.ReceiveCarrierEventCommand;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Empty configuration fails closed, so an accidentally unconfigured webhook
 * cannot mutate fulfilment.
 */
@Component
class HmacCarrierEventAuthenticator implements CarrierEventAuthenticator {
  private final String secret;

  HmacCarrierEventAuthenticator(@Value("${shipping.carrier.webhook-secret:}") String secret) {
    this.secret = secret;
  }

  @Override
  public boolean authentic(ReceiveCarrierEventCommand event, String signature) {
    if (secret.isBlank() || signature == null)
      return false;
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      String payload = String.join("|", event.carrier(), event.trackingReference(), event.carrierEventId(),
          event.status(), event.occurredAt().toString());
      byte[] expected = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      return MessageDigest.isEqual(expected, java.util.HexFormat.of().parseHex(signature));
    } catch (Exception ignored) {
      return false;
    }
  }
}

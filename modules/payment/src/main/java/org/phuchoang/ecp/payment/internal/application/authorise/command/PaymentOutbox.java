package org.phuchoang.ecp.payment.internal.application.authorise.command;

import java.time.Instant;
import java.util.UUID;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.payment.internal.domain.model.Payment;

public interface PaymentOutbox {
  void append(Payment payment, String eventType, IdentityActor actor, UUID correlationId, Instant occurredAt);
}

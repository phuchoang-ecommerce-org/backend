package org.phuchoang.ecp.payment.internal.application.authorise.command;

import java.net.URI;
import java.util.UUID;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;

public record InitiatePaymentCommand(IdentityActor caller, UUID orderId, String method, String idempotencyKey,
    URI returnUrl, UUID correlationId) {
}

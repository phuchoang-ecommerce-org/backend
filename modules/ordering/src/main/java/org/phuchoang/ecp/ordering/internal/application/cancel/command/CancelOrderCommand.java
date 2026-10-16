package org.phuchoang.ecp.ordering.internal.application.cancel.command;

import org.phuchoang.ecp.identity.api.authorization.IdentityActor;

import java.util.UUID;

public record CancelOrderCommand(IdentityActor caller, UUID orderId, String reason, UUID correlationId) { }

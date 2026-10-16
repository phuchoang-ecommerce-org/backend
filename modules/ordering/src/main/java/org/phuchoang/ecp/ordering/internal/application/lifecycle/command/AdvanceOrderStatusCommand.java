package org.phuchoang.ecp.ordering.internal.application.lifecycle.command;

import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.ordering.internal.domain.model.OrderStatus;

import java.util.UUID;

public record AdvanceOrderStatusCommand(IdentityActor caller, UUID orderId, OrderStatus target, String reason,
        UUID correlationId) { }

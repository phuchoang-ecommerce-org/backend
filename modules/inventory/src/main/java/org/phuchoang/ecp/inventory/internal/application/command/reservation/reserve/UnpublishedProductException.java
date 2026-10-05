package org.phuchoang.ecp.inventory.internal.application.command.reservation.reserve;

import java.util.UUID;

/** Raised when placement contains a product that is no longer published. */
public final class UnpublishedProductException extends RuntimeException {

    private final UUID orderLineId;

    public UnpublishedProductException(UUID orderLineId) {
        super("The product for order line " + orderLineId + " is no longer available.");
        this.orderLineId = orderLineId;
    }

    public UUID orderLineId() {
        return orderLineId;
    }
}

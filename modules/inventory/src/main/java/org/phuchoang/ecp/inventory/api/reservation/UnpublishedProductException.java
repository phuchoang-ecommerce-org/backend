package org.phuchoang.ecp.inventory.api.reservation;

import java.util.UUID;

/** The placement snapshot says this variant was withdrawn after checkout and before reservation. */
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

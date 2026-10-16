package org.phuchoang.ecp.shipping.internal.domain.model;

/** Monotonic carrier milestones. Their rank is the BR-SHP-02 ordering authority. */
public enum ShipmentStatus {
    CREATED(0), DISPATCHED(1), IN_TRANSIT(2), OUT_FOR_DELIVERY(3), DELIVERED(4), FAILED(4), RETURNED(5);

    private final int rank;

    ShipmentStatus(int rank) { this.rank = rank; }

    public int rank() { return rank; }
}

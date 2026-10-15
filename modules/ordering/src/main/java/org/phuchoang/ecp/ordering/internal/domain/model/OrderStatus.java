package org.phuchoang.ecp.ordering.internal.domain.model;

/** Legal lifecycle states for the Order aggregate (BR-ORD-01). */
public enum OrderStatus {
    DRAFT, PENDING_PAYMENT, PAID, PAYMENT_FAILED, PROCESSING, PACKED, SHIPPING, DELIVERED, COMPLETED, CANCELLED,
    RETURNED, REFUNDED
}

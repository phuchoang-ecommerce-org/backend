package org.phuchoang.ecp.ordering.internal.application.place.command;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Ordering-owned partnership port. Its adapter may become a saga participant after extraction. */
public interface StockReservationPort {
    List<Reservation> reserve(UUID orderId, List<Line> lines, Instant expiresAt);
    record Line(UUID orderLineId, UUID stockItemId, String sku, int quantity) { }
    record Reservation(UUID orderLineId, UUID stockItemId, UUID warehouseId, UUID reservationId, int quantity) { }
}

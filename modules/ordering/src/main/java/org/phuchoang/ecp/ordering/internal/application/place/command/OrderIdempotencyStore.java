package org.phuchoang.ecp.ordering.internal.application.place.command;

import java.util.Optional;
import java.util.UUID;

/** Transactional claim store. Database uniqueness, not a pre-flight lookup, decides concurrent submissions. */
public interface OrderIdempotencyStore {
    Optional<Entry> find(String key);
    boolean claim(String key, UUID customerId, String requestHash, UUID orderId);
    record Entry(UUID customerId, String requestHash, UUID orderId) { }
}

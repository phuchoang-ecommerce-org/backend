package org.phuchoang.ecp.cart.internal.application.cart.query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Application-owned observational port for Cart's persisted read model. */
public interface CartReadQuery {
    Optional<CartReadModel> readActiveByCustomerId(UUID customerId);
    Optional<CartReadModel> readActiveByGuestToken(String guestToken);
    Optional<CartReadModel> readById(UUID cartId);

    record CartReadModel(UUID id, UUID customerId, String guestToken, String status, Instant lastActivityAt,
                         Instant expiresAt, List<CartLineReadModel> lines) { }
    record CartLineReadModel(UUID id, UUID variantId, String sku, int quantity, Instant addedAt) { }
}

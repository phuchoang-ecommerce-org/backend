package org.phuchoang.ecp.cart.internal.infrastructure.persistence;

import org.phuchoang.ecp.cart.internal.application.cart.query.CartReadQuery;
import org.phuchoang.ecp.cart.internal.domain.model.CartStatus;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Observational Cart read adapter. It maps persistence rows directly to the application read model. */
@Repository
class JpaCartReadQueryAdapter implements CartReadQuery {
    private final CartJpaRepository carts;

    JpaCartReadQueryAdapter(CartJpaRepository carts) {
        this.carts = carts;
    }

    @Override
    public Optional<CartReadModel> readActiveByCustomerId(UUID customerId) {
        return carts.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE.name()).map(JpaCartReadQueryAdapter::toReadModel);
    }

    @Override
    public Optional<CartReadModel> readActiveByGuestToken(String guestToken) {
        return carts.findByGuestTokenAndStatus(guestToken, CartStatus.ACTIVE.name()).map(JpaCartReadQueryAdapter::toReadModel);
    }

    @Override
    public Optional<CartReadModel> readById(UUID cartId) {
        return carts.findAggregateById(cartId).map(JpaCartReadQueryAdapter::toReadModel);
    }

    private static CartReadModel toReadModel(CartEntity entity) {
        return new CartReadModel(entity.id(), entity.customerId(), entity.guestToken(), entity.status(), entity.lastActivityAt(),
            entity.expiresAt(), entity.lines().stream().map(line -> new CartLineReadModel(line.id(), line.variantId(),
                line.sku(), line.quantity(), line.addedAt())).toList());
    }
}

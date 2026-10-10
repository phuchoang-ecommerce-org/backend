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
    private final CartReadJpaMapper mapper;

    JpaCartReadQueryAdapter(CartJpaRepository carts, CartReadJpaMapper mapper) {
        this.carts = carts;
        this.mapper = mapper;
    }

    @Override
    public Optional<CartReadModel> readActiveByCustomerId(UUID customerId) {
        return carts.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE.name()).map(mapper::toReadModel);
    }

    @Override
    public Optional<CartReadModel> readActiveByGuestToken(String guestToken) {
        return carts.findByGuestTokenAndStatus(guestToken, CartStatus.ACTIVE.name()).map(mapper::toReadModel);
    }

    @Override
    public Optional<CartReadModel> readById(UUID cartId) {
        return carts.findAggregateById(cartId).map(mapper::toReadModel);
    }
}

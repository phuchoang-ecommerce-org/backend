package org.phuchoang.ecp.cart.internal.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.phuchoang.ecp.cart.internal.application.cart.query.CartReadQuery;
import org.phuchoang.ecp.cart.internal.domain.model.Cart;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CartJpaMapperTest {

    private final CartJpaMapper aggregateMapper = Mappers.getMapper(CartJpaMapper.class);
    private final CartReadJpaMapper readMapper = Mappers.getMapper(CartReadJpaMapper.class);

    @Test
    void mapsTheAggregateAndOwnedLinesWithoutLeakingJpaStateIntoTheDomain() {
        Instant addedAt = Instant.parse("2026-10-09T00:00:00Z");
        Instant persistedAt = Instant.parse("2026-10-09T00:05:00Z");
        Cart cart = Cart.guest(UUID.randomUUID(), "unguessable-token", addedAt, addedAt.plusSeconds(604800))
            .add(UUID.randomUUID(), "SKU-1", 2, addedAt);

        CartEntity entity = aggregateMapper.toEntity(cart, persistedAt);

        assertThat(entity.getCreatedAt()).isEqualTo(persistedAt);
        assertThat(entity.getUpdatedAt()).isEqualTo(persistedAt);
        assertThat(entity.getLines()).singleElement().satisfies(line -> {
            assertThat(line.getCart()).isSameAs(entity);
            assertThat(line.getCreatedAt()).isEqualTo(persistedAt);
            assertThat(line.getUpdatedAt()).isEqualTo(persistedAt);
        });
        assertThat(aggregateMapper.toDomain(entity)).isEqualTo(cart);

        CartReadQuery.CartReadModel readModel = readMapper.toReadModel(entity);
        assertThat(readModel).extracting(CartReadQuery.CartReadModel::id, CartReadQuery.CartReadModel::status)
            .containsExactly(cart.id(), "ACTIVE");
        assertThat(readModel.lines()).singleElement().extracting(CartReadQuery.CartLineReadModel::variantId,
            CartReadQuery.CartLineReadModel::quantity).containsExactly(cart.lines().getFirst().variantId(), 2);
    }
}

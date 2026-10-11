package org.phuchoang.ecp.cart.internal.domain.model;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.cart.internal.domain.repository.CartRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartTest {

    @Test
    void combinesAnExistingVariantIntoOneUnpricedLine() {
        UUID cartId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-09T00:00:00Z");
        Cart cart = Cart.guest(cartId, "opaque-guest-token", now, now.plusSeconds(7 * 86_400));

        Cart updated = cart.add(variantId, "SKU-RED-M", 2, now).add(variantId, "SKU-RED-M", 3, now.plusSeconds(1));

        assertThat(updated.lines()).singleElement().satisfies(line -> {
            assertThat(line.variantId()).isEqualTo(variantId);
            assertThat(line.quantity()).isEqualTo(5);
        });
    }

    @Test
    void aggregateLifecycleOperationsPersistTheirOwnStateTransitions() {
        CartRepository carts = mock(CartRepository.class);
        Instant now = Instant.parse("2026-10-09T00:00:00Z");
        Cart cart = Cart.guest(UUID.randomUUID(), "opaque-guest-token", now, now.plusSeconds(7 * 86_400));
        when(carts.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cart created = cart.create(carts);
        Cart updated = created.add(UUID.randomUUID(), "SKU-RED-M", 2, now, carts);
        Cart changed = updated.changeQuantity(updated.lines().getFirst().id(), 3, now.plusSeconds(1), carts);
        Cart removed = changed.remove(changed.lines().getFirst().id(), now.plusSeconds(2), carts);
        Cart replaced = updated.replaceLines(List.of(), now.plusSeconds(3), carts);
        Cart expired = updated.expire(carts);
        Cart merged = updated.mergeInto(UUID.randomUUID(), carts);

        verify(carts).save(cart);
        verify(carts).save(updated);
        verify(carts).save(changed);
        verify(carts).save(removed);
        verify(carts).save(replaced);
        verify(carts).save(expired);
        verify(carts).save(merged);
    }
}

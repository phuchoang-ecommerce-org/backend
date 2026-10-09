package org.phuchoang.ecp.cart.internal.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
}

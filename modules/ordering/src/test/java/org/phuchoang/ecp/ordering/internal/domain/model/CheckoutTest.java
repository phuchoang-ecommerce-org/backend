package org.phuchoang.ecp.ordering.internal.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutTest {

    @Test
    void checkoutCannotBeCreatedWithoutPurchasableLines() {
        assertThatThrownBy(() -> Checkout.start(UUID.randomUUID(), UUID.randomUUID(), "DRAFT-1", List.of()))
            .isInstanceOf(Checkout.NotPurchasable.class);
    }

    @Test
    void discountIsCappedAtTheSubtotal() {
        Checkout checkout = Checkout.start(UUID.randomUUID(), UUID.randomUUID(), "DRAFT-1", List.of(
            new CheckoutLine(UUID.randomUUID(), UUID.randomUUID(), "SKU-1", "Product", "Variant", 1,
                new Money(new BigDecimal("10.0000"), "USD"))));

        Checkout discounted = checkout.applyVoucher(UUID.randomUUID(), "SAVE", new BigDecimal("10.0001"));

        assertThat(discounted.discount().amount()).isEqualByComparingTo("10.0000");
        assertThat(discounted.total().amount()).isEqualByComparingTo("0.0000");
    }
}

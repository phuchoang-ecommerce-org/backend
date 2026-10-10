package org.phuchoang.ecp.cart.api;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.phuchoang.ecp.cart.internal.application.cart.command.AddCartLineCommand;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CartApiMapperTest {

    private final CartApiMapper mapper = Mappers.getMapper(CartApiMapper.class);

    @Test
    void mapsApplicationViewsAndCommandInputsToCartPublicContracts() {
        UUID cartId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-09T00:00:00Z");
        org.phuchoang.ecp.cart.internal.application.cart.query.MoneyView money =
            new org.phuchoang.ecp.cart.internal.application.cart.query.MoneyView(new BigDecimal("25.00"), "USD");
        org.phuchoang.ecp.cart.internal.application.cart.query.CartView applicationView =
            new org.phuchoang.ecp.cart.internal.application.cart.query.CartView(cartId, null, "ACTIVE", List.of(
                new org.phuchoang.ecp.cart.internal.application.cart.query.CartLineView(lineId, variantId, "SKU-1", "T-Shirt",
                    "Red / M", 2, money, money, false, 5, true, null, now)), money, now, now.plusSeconds(604800), false);

        CartView apiView = mapper.cartView(applicationView);
        AddCartLineCommand command = mapper.addCartLineCommand(new CartLineWrite(variantId, 2));

        assertThat(apiView).extracting(CartView::id, CartView::subtotal).containsExactly(cartId, new MoneyView(new BigDecimal("25.00"), "USD"));
        assertThat(apiView.lines()).singleElement().extracting(CartLineView::id, CartLineView::variantId, CartLineView::quantity)
            .containsExactly(lineId, variantId, 2);
        assertThat(command).isEqualTo(new AddCartLineCommand(variantId, 2));
    }
}

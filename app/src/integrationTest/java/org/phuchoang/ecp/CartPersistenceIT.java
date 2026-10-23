package org.phuchoang.ecp;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.cart.api.CartFacade;
import org.phuchoang.ecp.cart.api.CartLineWrite;
import org.phuchoang.ecp.cart.api.CartNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** FR-CRT-01/02/05: guest cart writes persist and remain private to the guest token. */
@SpringBootTest(properties = {
    "ecp.cursor.active-key=integration-test-cursor-secret-0001",
    "spring.flyway.default-schema=cart_persistence_l4",
    "spring.flyway.schemas=cart_persistence_l4",
    "spring.datasource.hikari.schema=cart_persistence_l4",
    "spring.datasource.hikari.connection-init-sql=SET search_path TO cart_persistence_l4",
    "spring.jpa.properties.hibernate.default_schema=cart_persistence_l4",
    "spring.task.scheduling.enabled=false",
    "spring.kafka.listener.auto-startup=false",
    "ecp.outbox.poll-delay-ms=600000"
})
@Import(TestcontainersConfiguration.class)
class CartPersistenceIT {

    @Autowired private CartFacade carts;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void guestLineSurvivesReadAndQuantityChangeButAnotherGuestCannotSeeIt() {
        UUID categoryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        String sku = "CRT-" + UUID.randomUUID();
        jdbc.update("insert into cart_persistence_l4.catalog_category (id, name, slug, path) values (?, ?, ?, ?)",
            categoryId, "Cart test category", "cart-" + categoryId, "/cart-" + categoryId + "/");
        jdbc.update("insert into cart_persistence_l4.catalog_product "
            + "(id, category_id, name, slug, publication_status, published_at) "
            + "values (?, ?, ?, ?, 'PUBLISHED', now())",
            productId, categoryId, "Cart test product", "cart-product-" + productId);
        jdbc.update("insert into cart_persistence_l4.catalog_variant "
            + "(id, product_id, sku, name, list_price_amount, list_price_currency) values (?, ?, ?, ?, ?, ?)",
            variantId, productId, sku, "Standard", new BigDecimal("7.5000"), "USD");
        String guestToken = "guest-" + UUID.randomUUID();

        UUID cartId = carts.getCurrentCart(null, guestToken).id();
        UUID lineId = carts.addCartLine(null, guestToken, cartId, new CartLineWrite(variantId, 1))
            .lines().getFirst().id();
        carts.updateCartLineQuantity(null, guestToken, cartId, lineId, 3);

        assertThat(carts.getCart(null, guestToken, cartId).lines().getFirst().quantity()).isEqualTo(3);
        assertThat(jdbc.queryForObject("select quantity from cart_persistence_l4.cart_cart_line where id = ?",
            Integer.class, lineId)).isEqualTo(3);
        assertThatThrownBy(() -> carts.getCart(null, "another-" + UUID.randomUUID(), cartId))
            .isInstanceOf(CartNotFoundException.class);
    }
}

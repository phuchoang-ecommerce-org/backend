package org.phuchoang.ecp;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.catalog.api.administration.category.CategoryWrite;
import org.phuchoang.ecp.catalog.api.administration.product.ProductWrite;
import org.phuchoang.ecp.catalog.api.administration.product.VariantWrite;
import org.phuchoang.ecp.catalog.api.error.DomainException;
import org.phuchoang.ecp.catalog.api.error.GenErrorCode;
import org.phuchoang.ecp.catalog.api.facade.CatalogAdministrationFacade;
import org.phuchoang.ecp.catalog.api.view.common.MoneyView;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** BR-CAT-01: a SKU belongs to only one catalog variant. */
@SpringBootTest(properties = {
    "ecp.cursor.active-key=integration-test-cursor-secret-0001",
    "spring.flyway.default-schema=catalog_administration_l4",
    "spring.flyway.schemas=catalog_administration_l4",
    "spring.datasource.hikari.schema=catalog_administration_l4",
    "spring.datasource.hikari.connection-init-sql=SET search_path TO catalog_administration_l4",
    "spring.jpa.properties.hibernate.default_schema=catalog_administration_l4",
    "spring.task.scheduling.enabled=false",
    "spring.kafka.listener.auto-startup=false",
    "ecp.outbox.poll-delay-ms=600000"
})
@Import(TestcontainersConfiguration.class)
class CatalogAdministrationIT {

    @Autowired private CatalogAdministrationFacade catalog;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void createsProductAndVariantButRejectsDuplicateSku() {
        IdentityActor administrator = new IdentityActor(UUID.randomUUID(), Set.of("ADMINISTRATOR"));
        UUID categoryId = catalog.createCategory(administrator, UUID.randomUUID(),
            new CategoryWrite("Integration " + UUID.randomUUID(), null, null, 0, false)).id();
        UUID firstProduct = catalog.createProduct(administrator, UUID.randomUUID(),
            new ProductWrite("First product", "Test", "ECP", categoryId, Map.of())).id();
        UUID secondProduct = catalog.createProduct(administrator, UUID.randomUUID(),
            new ProductWrite("Second product", "Test", "ECP", categoryId, Map.of())).id();
        String sku = "CAT-" + UUID.randomUUID();
        VariantWrite variant = new VariantWrite(sku, "Standard", new MoneyView(new BigDecimal("12.5000"), "USD"),
            Map.of(), 100, true);

        UUID variantId = catalog.addVariant(administrator, UUID.randomUUID(), firstProduct, variant).id();

        assertThat(jdbc.queryForObject("select product_id from catalog_administration_l4.catalog_variant where id = ?",
            UUID.class, variantId)).isEqualTo(firstProduct);
        assertThatThrownBy(() -> catalog.addVariant(administrator, UUID.randomUUID(), secondProduct, variant))
            .isInstanceOfSatisfying(DomainException.class,
                exception -> assertThat(exception.errorCode()).isEqualTo(GenErrorCode.VALIDATION_FAILED));
        assertThat(jdbc.queryForObject("select count(*) from catalog_administration_l4.catalog_variant where sku = ?",
            Integer.class, sku)).isEqualTo(1);
    }
}
